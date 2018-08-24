package utils;

import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PrintException {
    public static void Print(Logger logger, Exception e){
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        logger.error(stringWriter.toString());
    }
    public static void Print(Logger logger, Exception e, String message){
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message).append(stringWriter.toString());
        logger.error(stringBuilder.toString());
    }

}
