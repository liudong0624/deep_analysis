package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Check_Sum {
    private static Logger logger = LoggerFactory.getLogger(Check_Sum.class);
    public Check_Sum(){}
    public static String Md5(byte [] in){
        String strMD5 = null;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            PrintException.Print(logger,e);
        }

        digest.update(in, 0, in.length);

        BigInteger bigInteger = new BigInteger(1, digest.digest());
        strMD5  = bigInteger.toString(16);
        return strMD5;
    }
}
