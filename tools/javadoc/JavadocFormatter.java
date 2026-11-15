import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 使用 JDK 21 语法树识别声明、词法扫描识别注释的 JavaDoc 格式化工具。
 * 仅修改声明注释与 record 组件相邻空白，区分组件块注释与类字段 JavaDoc。
 */
public final class JavadocFormatter {
    /**
     * 注释的原始字节字符范围与内容类型
     */
    private record Comment(int start, int end, boolean javadoc) { }
    /**
     * 语法树声明起点与字段分类
     */
    private record Declaration(int start, boolean field, boolean recordComponent) { }
    /**
     * 单个文档注释的替换内容
     */
    private record Edit(int start, int end, String replacement, boolean layout) {
        /**
         * 创建不涉及声明排版的注释替换。
         * @param start 起点
         * @param end 终点
         * @param replacement 注释文本
         */
        private Edit(int start, int end, String replacement) { this(start, end, replacement, false); }
    }

    /**
     * 执行可复核的检查或写入，写入前保存受影响文件副本和 SHA-256 审计报告。
     * @param args 项目根目录，以及可选的 --write、相对备份目录
     * @throws Exception 语法、文件访问或内容不变验证失败
     */
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        boolean write = args.length > 1 && args[1].equals("--write");
        Path backupRoot = root.resolve(args.length > 2 ? args[2] : ".cache/javadoc-before").normalize();
        if (!backupRoot.startsWith(root) || backupRoot.equals(root)) throw new IllegalArgumentException("Backup directory must be inside project root");
        List<Path> files;
        try (var paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> eligible(root.relativize(p))).sorted().toList();
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> report = new ArrayList<>();
        int docs = 0, layouts = 0, protectedFields = 0, unattached = 0;
        try (var manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--release", "21"), null, manager.getJavaFileObjectsFromPaths(files));
            var trees = Trees.instance(task);
            var units = new ArrayList<CompilationUnitTree>();
            task.parse().forEach(units::add);
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IllegalStateException("Refusing to edit sources with parse errors: " + diagnostics.getDiagnostics());
            }
            for (var unit : units) {
                Path file = Path.of(unit.getSourceFile().toUri());
                String source = Files.readString(file, StandardCharsets.UTF_8);
                String newline = source.contains("\r\n") ? "\r\n" : "\n";
                List<Declaration> declarations = declarations(unit, trees);
                List<Edit> edits = new ArrayList<>();
                for (var comment : comments(source)) {
                    if (!source.startsWith("/*", comment.start())) continue;
                    Declaration declaration = declarations.stream().filter(d -> d.start() >= comment.end()).findFirst().orElse(null);
                    if (declaration == null || !withoutComments(source.substring(comment.end(), declaration.start())).isBlank()) {
                        if (comment.javadoc()) unattached++;
                        continue;
                    }
                    if (!comment.javadoc() && !declaration.field()) continue;
                    String original = source.substring(comment.start(), comment.end());
                    List<String> content = content(original);
                    boolean compact = declaration.field() && compactable(content);
                    if (declaration.field() && !compact) protectedFields++;
                    int lineStart = source.lastIndexOf('\n', comment.start() - 1) + 1;
                    String indent = source.substring(lineStart, comment.start());
                    if (!indent.isBlank()) indent = " ".repeat((int) source.substring(lineStart, comment.start()).chars().takeWhile(Character::isWhitespace).count());
                    String opening = declaration.recordComponent() ? "/* " : "/** ";
                    String replacement = compact ? opening + content.stream().map(String::strip).collect(Collectors.joining(" ")) + " */"
                            : multiline(content, indent, newline);
                    if (declaration.recordComponent() && !compact) replacement = "/*" + replacement.substring(3);
                    if (!original.equals(replacement)) edits.add(new Edit(comment.start(), comment.end(), replacement));
                }
                edits.addAll(recordLayout(unit, trees, source, newline));
                if (edits.isEmpty()) continue;
                edits.sort(Comparator.comparingInt(Edit::start).thenComparingInt(Edit::end));
                StringBuilder updated = new StringBuilder(source);
                StringBuilder commentsOnly = new StringBuilder(source);
                for (int i = edits.size() - 1; i >= 0; i--) {
                    var edit = edits.get(i);
                    updated.replace(edit.start(), edit.end(), edit.replacement());
                    if (!edit.layout()) commentsOnly.replace(edit.start(), edit.end(), edit.replacement());
                }
                String result = updated.toString();
                if (!withoutComments(source).equals(withoutComments(commentsOnly.toString()))) {
                    throw new IllegalStateException("Non-comment text changed: " + file);
                }
                // 双重检查注释语义文本，包含链接、标签和代码片段的全部非空白字符必须保持不变。
                for (var edit : edits) {
                    if (edit.layout()) {
                        if (!source.substring(edit.start(), edit.end()).isBlank() || !edit.replacement().isBlank()) {
                            throw new IllegalStateException("Layout edit contains non-whitespace: " + file);
                        }
                        continue;
                    }
                    if (!semantic(source.substring(edit.start(), edit.end())).equals(semantic(edit.replacement()))) {
                        throw new IllegalStateException("Comment content changed: " + file);
                    }
                }
                Path relative = root.relativize(file);
                if (write) {
                    Path backup = backupRoot.resolve(relative);
                    Files.createDirectories(backup.getParent());
                    if (Files.exists(backup)) throw new IllegalStateException("Backup already exists; inspect previous run before rewriting: " + backup);
                    Files.writeString(backup, source, StandardCharsets.UTF_8);
                    Files.writeString(file, result, StandardCharsets.UTF_8);
                }
                long commentCount = edits.stream().filter(edit -> !edit.layout()).count();
                long layoutCount = edits.size() - commentCount;
                report.add("{\"file\":\"" + relative.toString().replace('\\', '/') + "\",\"comments\":" + commentCount + ",\"layoutEdits\":" + layoutCount
                        + ",\"nonCommentSha256\":\"" + hash(withoutComments(source)) + "\"}");
                docs += (int) commentCount;
                layouts += (int) layoutCount;
            }
        }
        String summary = "{\"scannedFiles\":" + files.size() + ",\"changedFiles\":" + report.size() + ",\"changedComments\":" + docs
                + ",\"layoutEdits\":" + layouts
                + ",\"protectedMultilineFields\":" + protectedFields + ",\"unattachedComments\":" + unattached + ",\"files\":[\n"
                + String.join(",\n", report) + "\n]}\n";
        Path output = root.resolve(write ? ".cache/javadoc-format-report.json" : ".cache/javadoc-check-report.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, summary, StandardCharsets.UTF_8);
        System.out.println("Scanned " + files.size() + " Java files; " + report.size() + " files / " + docs + " comments / " + layouts + " layout edits "
                + (write ? "formatted" : "require formatting") + "; protected multiline fields=" + protectedFields + "; unattached=" + unattached);
    }

    /**
     * 排除构建目录、依赖、版本库及工具缓存。
     * @param relative 项目相对路径
     * @return 是否属于本项目源代码
     */
    private static boolean eligible(Path relative) {
        for (Path part : relative) {
            if (Set.of("target", "build", "out", ".git", ".cache", ".idea", "node_modules", "vendor", "generated", "generated-sources").contains(part.toString())) return false;
        }
        return true;
    }

    /**
     * 将 record 组件注释、定义拆成独立行，并在相邻组件之间保留一个空行。
     * 只生成语法树声明边界处的空白替换，不改组件、注解、字符串或表达式。
     * @param unit 编译单元
     * @param trees 语法树位置服务
     * @param source 原始源码
     * @param newline 原有换行符
     * @return 仅包含空白字符的替换
     */
    private static List<Edit> recordLayout(CompilationUnitTree unit, Trees trees, String source, String newline) {
        List<Edit> edits = new ArrayList<>();
        List<Comment> allComments = comments(source);
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void unused) {
                if (node.getKind() == Tree.Kind.RECORD) {
                    var components = node.getMembers().stream().filter(member -> member instanceof VariableTree)
                            .map(member -> (VariableTree) member)
                            .filter(member -> !member.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.STATIC)).toList();
                    int classStart = (int) trees.getSourcePositions().getStartPosition(unit, node);
                    int classLine = source.lastIndexOf('\n', classStart) + 1;
                    String classIndent = source.substring(classLine, classStart).replaceFirst("[^ \\t].*", "");
                    for (int i = 0; i < components.size(); i++) {
                        int start = (int) trees.getSourcePositions().getStartPosition(unit, components.get(i));
                        Comment attached = null;
                        for (var comment : allComments) {
                            if (comment.end() <= start && source.startsWith("/*", comment.start())
                                    && source.substring(comment.end(), start).isBlank()) attached = comment;
                        }
                        if (attached == null) continue;
                        int lineStart = source.lastIndexOf('\n', attached.start() - 1) + 1;
                        String indent = source.substring(lineStart, attached.start());
                        if (!indent.isBlank()) indent = classIndent + "    ";
                        int prefix = attached.start();
                        while (prefix > 0 && Character.isWhitespace(source.charAt(prefix - 1))) prefix--;
                        String before = newline.repeat(i == 0 ? 1 : 2) + indent;
                        if (!source.substring(prefix, attached.start()).equals(before)) edits.add(new Edit(prefix, attached.start(), before, true));
                        String between = newline + indent;
                        if (!source.substring(attached.end(), start).equals(between)) edits.add(new Edit(attached.end(), start, between, true));
                    }
                }
                return super.visitClass(node, unused);
            }
        }.scan(unit, null);
        return edits;
    }

    /**
     * 读取声明起点，区分 record 组件与普通字段；record 的静态字段仍按类字段处理。
     * @param unit 编译单元
     * @param trees 语法树位置服务
     * @return 按源位置排序的声明
     */
    private static List<Declaration> declarations(CompilationUnitTree unit, Trees trees) {
        List<Declaration> result = new ArrayList<>();
        new TreePathScanner<Void, Void>() {
            private void add(Tree tree, boolean field, boolean recordComponent) {
                long start = trees.getSourcePositions().getStartPosition(unit, tree);
                if (start >= 0) result.add(new Declaration((int) start, field, recordComponent));
            }
            @Override public Void visitClass(ClassTree node, Void unused) { add(node, false, false); return super.visitClass(node, unused); }
            @Override public Void visitMethod(MethodTree node, Void unused) { add(node, false, false); return super.visitMethod(node, unused); }
            @Override public Void visitPackage(PackageTree node, Void unused) { add(node, false, false); return super.visitPackage(node, unused); }
            @Override public Void visitVariable(VariableTree node, Void unused) {
                Tree parent = getCurrentPath().getParentPath().getLeaf();
                if (parent instanceof ClassTree) {
                    boolean component = parent.getKind() == Tree.Kind.RECORD
                            && !node.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.STATIC);
                    add(node, true, component);
                }
                return super.visitVariable(node, unused);
            }
        }.scan(unit, null);
        result.sort(Comparator.comparingInt(Declaration::start));
        return result;
    }

    /**
     * 词法扫描注释；跳过字符串、字符和文本块，避免误改代码中的注释示例。
     * @param source Java 源码
     * @return 全部注释位置
     */
    private static List<Comment> comments(String source) {
        List<Comment> result = new ArrayList<>();
        int n = source.length();
        for (int i = 0; i < n;) {
            if (source.startsWith("\"\"\"", i)) {
                i += 3;
                while (i < n && !source.startsWith("\"\"\"", i)) { if (source.charAt(i) == '\\') i++; i++; }
                i = Math.min(n, i + 3);
            } else if (source.charAt(i) == '"' || source.charAt(i) == '\'') {
                char quote = source.charAt(i++);
                while (i < n) { char c = source.charAt(i++); if (c == '\\') i++; else if (c == quote) break; }
            } else if (source.startsWith("//", i)) {
                int end = source.indexOf('\n', i + 2); if (end < 0) end = n;
                result.add(new Comment(i, end, false)); i = end;
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2); if (end < 0) throw new IllegalArgumentException("Unclosed Java comment");
                result.add(new Comment(i, end + 2, source.startsWith("/**", i) && !source.startsWith("/**/", i))); i = end + 2;
            } else i++;
        }
        return result;
    }

    /**
     * 去除注释范围，保留范围外的每个字符供内容不变审计。
     * @param source Java 源码
     * @return 不含注释的原始文本
     */
    private static String withoutComments(String source) {
        StringBuilder result = new StringBuilder(); int previous = 0;
        for (var comment : comments(source)) { result.append(source, previous, comment.start()); previous = comment.end(); }
        return result.append(source, previous, source.length()).toString();
    }

    /**
     * 提取声明注释正文，仅移除注释边界和行首星号，保留代码块内部缩进。
     * @param comment 完整声明注释
     * @return 正文行
     */
    private static List<String> content(String comment) {
        String body = comment.substring(comment.startsWith("/**") ? 3 : 2, comment.length() - 2);
        var lines = new ArrayList<>(Arrays.asList(body.split("\\R", -1)));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0) line = line.stripLeading();
            else line = line.replaceFirst("^[ \\t]*\\* ?", "");
            lines.set(i, line.stripTrailing());
        }
        while (!lines.isEmpty() && lines.getFirst().isBlank()) lines.removeFirst();
        while (!lines.isEmpty() && lines.getLast().isBlank()) lines.removeLast();
        return lines;
    }

    /**
     * 保护含代码、HTML 布局、标准块标签和列表的复杂字段说明。
     * @param lines 正文行
     * @return 是否可安全合为一行
     */
    private static boolean compactable(List<String> lines) {
        if (lines.size() <= 1) return true;
        return lines.stream().noneMatch(line -> line.isBlank() || line.stripLeading().startsWith("@")
                || line.matches(".*(?:<[^>]+>|\\{@code|\\{@snippet).*" ) || line.stripLeading().matches("(?:[-*]|[0-9]+[.)]) .*"));
    }

    /**
     * 构造标准多行文档，不重排正文或标准标签。
     * @param lines 正文行
     * @param indent 声明缩进
     * @param newline 文件原有换行符
     * @return 多行 JavaDoc
     */
    private static String multiline(List<String> lines, String indent, String newline) {
        StringBuilder result = new StringBuilder("/**");
        for (String line : lines) result.append(newline).append(indent).append(" *").append(line.isEmpty() ? "" : " " + line);
        return result.append(newline).append(indent).append(" */").toString();
    }

    /**
     * 用于核对注释内容，仅忽略布局空白。
     * @param comment JavaDoc
     * @return 去除布局空白的正文
     */
    private static String semantic(String comment) { return String.join("", content(comment)).replaceAll("\\s", ""); }

    /**
     * 生成不含注释文本的审计摘要。
     * @param value 文本
     * @return SHA-256 十六进制
     * @throws Exception JVM 不支持摘要算法
     */
    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
