package com.jd.live.agent.implement.auth.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public abstract class AsymmetricAlgorithmBuilder<T extends PublicKey, V extends PrivateKey> implements AlgorithmBuilder {

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
    public Algorithm create(AlgorithmContext context) throws Exception {
        if (instance == null) {
            return null;
        }
        byte[] publicDer = context.getRole() != AlgorithmRole.VERIFY ? null : context.loadPublicKey();
        byte[] privateDer = context.getRole() != AlgorithmRole.SIGNATURE ? null : context.loadPrivateKey();
        if (publicDer == null && privateDer == null) {
            return null;
        }
        T publicKey = publicDer == null ? null : (T) instance.generatePublic(new X509EncodedKeySpec(publicDer));
        V privateKey = privateDer == null ? null : (V) instance.generatePrivate(new PKCS8EncodedKeySpec(privateDer));
        return doCreate(publicKey, privateKey);
    }

    protected abstract Algorithm doCreate(T publicKey, V privateKey);
}
