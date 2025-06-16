package com.jd.live.agent.implement.auth.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.policy.service.auth.JWTAlgorithmContext;
import com.jd.live.agent.governance.policy.service.auth.JWTAlgorithmRole;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Base class for asymmetric JWT algorithm builders using public/private key pairs.
 * <p>
 * Handles key conversion from DER format and delegates algorithm-specific
 * creation to concrete implementations.
 *
 * @param <T> Type of public key
 * @param <T> Type of private key
 */
public abstract class AsymmetricAlgorithmBuilder<T extends PublicKey, V extends PrivateKey> implements JWTAlgorithmBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AsymmetricAlgorithmBuilder.class);

    protected KeyFactory instance;

    public AsymmetricAlgorithmBuilder(String algorithm) {
        try {
            instance = KeyFactory.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to create KeyFactory instance for algorithm: {} ", algorithm, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Algorithm create(JWTAlgorithmContext context) throws Exception {
        if (instance == null) {
            return null;
        }
        byte[] publicDer = context.getRole() != JWTAlgorithmRole.VERIFY ? null : context.loadPublicKey();
        byte[] privateDer = context.getRole() != JWTAlgorithmRole.SIGNATURE ? null : context.loadPrivateKey();
        if (publicDer == null && privateDer == null) {
            return null;
        }
        T publicKey = publicDer == null ? null : (T) instance.generatePublic(new X509EncodedKeySpec(publicDer));
        V privateKey = privateDer == null ? null : (V) instance.generatePrivate(new PKCS8EncodedKeySpec(privateDer));
        return doCreate(publicKey, privateKey);
    }

    /**
     * Algorithm-specific instance creation.
     *
     * @param publicKey  Public key (may be null for signing-only contexts)
     * @param privateKey Private key (may be null for verify-only contexts)
     * @return Configured algorithm instance
     */
    protected abstract Algorithm doCreate(T publicKey, V privateKey);
}
