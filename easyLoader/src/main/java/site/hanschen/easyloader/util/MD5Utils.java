/*
 * Copyright 2016 Hans Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package site.hanschen.easyloader.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MD5Utils {

    /**
     * 获得字符串md5值，注意，不要使用{@link DigestUtils#md5Hex(String)}方法直接返回字符串，会报NoSuchMethodError异常，因为有些android内置的类没有这个方法
     *
     * @param input 需要加密的字符串
     * @return 32位MD5值
     */
    public static String getMD5(String input) {
        return new String(Hex.encodeHex(DigestUtils.md5(input)));
    }

    /**
     * 获得文件md5值，注意，不要使用{@link DigestUtils#md5Hex(InputStream)}方法直接返回字符串，会报NoSuchMethodError异常，因为有些android内置的类没有这个方法
     *
     * @param file 需要加密的文件
     * @return 32位MD5值
     */
    public static String getMD5(File file) throws IOException {
        return new String(Hex.encodeHex(DigestUtils.md5(new FileInputStream(file))));
    }
}
