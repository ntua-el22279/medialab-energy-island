package gr.ntua.medialab.energyisland.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A candidate site where at most one catalog item may be built
 * ({@code sites} in {@code island.json}).
 *
 * @param id       unique id, e.g. {@code s1}
 * @param position cell (land or mountain)
 * @param allowed  technologies allowed here; empty is representable but invalid
 */
public record Site(String id, Position position, Set<TechType> allowed) implements PlacedElement {

    /**
     * Creates a site, keeping a read-only copy of {@code allowed} in enum order.
     */
    public Site {
        allowed = Collections.unmodifiableSet(copyOf(allowed));
    }

    private static EnumSet<TechType> copyOf(Collection<TechType> types) {
        return types.isEmpty() ? EnumSet.noneOf(TechType.class) : EnumSet.copyOf(types);
    }

    /**
     * Tells whether a technology may be built at this site.
     *
     * @param type the technology
     * @return {@code true} if it is in {@code allowed}
     */
    public boolean allows(TechType type) {
        return allowed.contains(type);
    }

    @Override
    public Site withPosition(Position newPosition) {
        return new Site(id, newPosition, allowed);
    }
}
