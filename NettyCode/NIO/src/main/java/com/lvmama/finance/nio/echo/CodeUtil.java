package com.lvmama.finance.nio.echo;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author <a href="mailto:huangyuze@lvmama.com">hyz</a> <br>
 * @date 2020/7/28 - 14:17
 * @since
 */
public final class CodeUtil {
    private static Charset charset = Charset.forName("UTF-8");

    private CodeUtil() {}

    // 解码
    public static String decode(ByteBuffer byteBuffer) {
        return charset.decode(byteBuffer).toString();
    }

    // 编码
    public static ByteBuffer encode(String str) {
        return charset.encode(str);
    }
}
