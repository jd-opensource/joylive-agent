package com.jd.live.agent.governance.db;

import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a potential database connection candidate with failover support.
 * Tracks original address/nodes and actual database instance, with optional failover address.
 */
@Getter
public class DbCandidate {

    private final String type;

    private final AccessMode accessMode;

    private final String oldAddress;

    private final String[] oldNodes;

    private final LiveDatabase database;

    private final Function<LiveDatabase, String> addressResolver;

    private final String newAddress;

    private final String[] newNodes;

    private final boolean redirected;

    public DbCandidate(String type,
                       AccessMode accessMode,
                       String oldAddress,
                       String[] oldNodes,
                       LiveDatabase database,
                       Function<LiveDatabase, String> addressResolver) {
        this.type = type;
        this.accessMode = accessMode;
        this.oldAddress = oldAddress;
        this.oldNodes = oldNodes;
        this.database = database;
        this.addressResolver = addressResolver;
        this.newAddress = database == null ? oldAddress : (addressResolver == null ? database.getPrimaryAddress() : addressResolver.apply(database));
        this.newNodes = database == null ? oldNodes : database.getNodes().toArray(new String[0]);
        this.redirected = database != null && !database.contains(oldNodes);
    }

    /**
     * Determines if database address has changed by comparing with target candidate.
     *
     * @param target Candidate to compare against (nullable)
     * @return true if either candidate is null, instances differ, or addresses don't match
     */
    public boolean isChanged(DbCandidate target) {
        if (target == null) {
            return true;
        }
        LiveDatabase targetDatabase = target.database;
        if (targetDatabase == database) {
            return false;
        }
        List<String> targetAddresses = targetDatabase == null ? null : targetDatabase.getAddresses();
        List<String> Addresses = database == null ? null : database.getAddresses();
        return !Objects.equals(targetAddresses, Addresses);
    }

}
