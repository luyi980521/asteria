package io.asteria.ledger.application.assembler;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.ledger.application.command.PostingCommand;
import io.asteria.ledger.domain.entity.Posting;
import io.asteria.ledger.domain.valueobject.PostingId;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 分录对象转换器
 * */
@Component
@AllArgsConstructor
public class PostingAssembler {

    private final DistributedIdGenerator distributedIdGenerator;

    public Posting toEntity(PostingCommand command) {
        PostingId postingId = PostingId.of(
                distributedIdGenerator.nextId()
        );

        return Posting.create(
                postingId,
                command.ledgerAccountId(),
                command.money(),
                command.direction()
        );
    }
}
