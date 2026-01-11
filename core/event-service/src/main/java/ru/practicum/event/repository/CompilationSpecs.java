package ru.practicum.event.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.event.entity.Compilation;

public class CompilationSpecs {

    public static Specification<Compilation> isPinned(boolean value) {
        return (root, query, builder) ->
                builder.equal(root.get("pinned"), value);
    }
}
