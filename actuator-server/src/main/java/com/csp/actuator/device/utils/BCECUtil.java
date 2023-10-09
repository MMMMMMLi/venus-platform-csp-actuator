package com.csp.actuator.device.utils;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * @Auth yulang
 * @Date 2022/11/30
 * @Version 1.0
 */
public class BCECUtil {
    private static final String ALGO_NAME_EC = "EC";

    /**
     * @param domainParams
     * @return
     */
    public static int getCurveLength(ECDomainParameters domainParams) {
        return (domainParams.getCurve().getFieldSize() + 7) / 8;
    }

    /**
     * @param curveLength
     * @param src
     * @return
     */
    public static byte[] fixToCurveLengthBytes(int curveLength, byte[] src) {
        if (src.length == curveLength) {
            return src;
        }
        byte[] result = new byte[curveLength];
        if (src.length > curveLength) {
            System.arraycopy(src, src.length - result.length, result, 0, result.length);
        } else {
            System.arraycopy(src, 0, result, result.length - src.length, src.length);
        }
        return result;
    }

    /**
     * @param x
     * @param y
     * @param curve
     * @param domainParameters
     * @return
     */
    public static ECPublicKeyParameters createECPublicKeyParameters(
            BigInteger x, BigInteger y, ECCurve curve, ECDomainParameters domainParameters) {
        return createECPublicKeyParameters(x.toByteArray(), y.toByteArray(), curve, domainParameters);
    }

    public static BCECPrivateKey convertPKCS8ToECPrivateKey(byte[] pkcs8Key)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        PKCS8EncodedKeySpec peks = new PKCS8EncodedKeySpec(pkcs8Key);
        KeyFactory kf = KeyFactory.getInstance(ALGO_NAME_EC, BouncyCastleProvider.PROVIDER_NAME);
        return (BCECPrivateKey) kf.generatePrivate(peks);
    }

    public static ECPublicKeyParameters convertPublicKeyToParameters(BCECPublicKey ecPubKey) {
        ECParameterSpec parameterSpec = ecPubKey.getParameters();
        ECDomainParameters domainParameters = new ECDomainParameters(parameterSpec.getCurve(), parameterSpec.getG(),
                parameterSpec.getN(), parameterSpec.getH());
        return new ECPublicKeyParameters(ecPubKey.getQ(), domainParameters);
    }
    /**
     * @param xHex
     * @param yHex
     * @param curve
     * @param domainParameters
     * @return
     */
    public static ECPublicKeyParameters createECPublicKeyParameters(
            String xHex, String yHex, ECCurve curve, ECDomainParameters domainParameters) {
        return createECPublicKeyParameters(ByteUtils.fromHexString(xHex), ByteUtils.fromHexString(yHex),
                curve, domainParameters);
    }

    /**
     * @param xBytes
     * @param yBytes
     * @param curve
     * @param domainParameters
     * @return
     */
    public static ECPublicKeyParameters createECPublicKeyParameters(
            byte[] xBytes, byte[] yBytes, ECCurve curve, ECDomainParameters domainParameters) {
        final byte uncompressedFlag = 0x04;
        int curveLength = getCurveLength(domainParameters);
        xBytes = fixToCurveLengthBytes(curveLength, xBytes);
        yBytes = fixToCurveLengthBytes(curveLength, yBytes);
        byte[] encodedPubKey = new byte[1 + xBytes.length + yBytes.length];
        encodedPubKey[0] = uncompressedFlag;
        System.arraycopy(xBytes, 0, encodedPubKey, 1, xBytes.length);
        System.arraycopy(yBytes, 0, encodedPubKey, 1 + xBytes.length, yBytes.length);
        return new ECPublicKeyParameters(curve.decodePoint(encodedPubKey), domainParameters);
    }
}
