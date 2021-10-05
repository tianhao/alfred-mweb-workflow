package me.chenzz.mweb.format;

import java.io.File;
import java.io.IOException;
import java.util.Stack;
import me.chenzz.mweb.format.utils.DateFormatUtils;
import me.chenzz.mweb.format.utils.FileUtils;
import me.chenzz.mweb.format.utils.StringUtils;


public class Main {

    public static void main(String[] args) throws IOException {

        String pathPrefix = "/Users/chenzz/Library/Containers/com.coderforart.MWeb3/Data/Library/Application Support/MWebLibrary/docs";
        String fileName = "15018632272245";
        String filePath = pathPrefix + "/" + fileName + ".md";
        if (args.length >= 1 && !StringUtils.isEmpty(args[0])) {
            filePath = args[0];
        }

        System.out.println("filePath=" + filePath);


        String content = FileUtils.readFileToString(filePath, "UTF-8");

        // 先备份一下
        String backupFileName = DateFormatUtils.format(System.currentTimeMillis(), "yyyy年MM月dd日 HH时mm分ss秒");
        String path = "/tmp/mweb-format/" + backupFileName + ".md";
        FileUtils.writeStringToFile(path, content, "UTF-8");


        String[] lineArr = content.split("\n");
        Integer currentTitleLevel = null;
        int currentNo = 0;
        Stack<TitleInfo> titleNoStack = new Stack<>();

        for (int i = 0; i < lineArr.length; i++) {
            String line = lineArr[i];

            if (!isTitle(line)) {
                continue;
            }

            int titleLevel = getTitleLevel(line);
            if (null == currentTitleLevel) {
                currentTitleLevel = titleLevel;
            }

            if (titleLevel == currentTitleLevel) {
                // 如果本行层级 和 当前的层级一致
                currentNo++;
                String newLine = generateNewLine(currentNo, titleNoStack, line);
                lineArr[i] = newLine;
            } else if (titleLevel > currentTitleLevel) {
                // 如果本行层级 小于 当前的层级 (2级标题 小于 1级标题)

                TitleInfo titleInfo = new TitleInfo(currentNo, currentTitleLevel);

                titleNoStack.push(titleInfo);
                currentTitleLevel = titleLevel;
                currentNo = 0;

                currentNo++;
                String newLine = generateNewLine(currentNo, titleNoStack, line);
                lineArr[i] = newLine;
            } else {
                // 如果本行层级 大于 当前的层级
                TitleInfo titleInfo = titleNoStack.pop();
                currentNo = titleInfo.getTitleNo();
                currentTitleLevel = titleInfo.getTitleLevel();

                currentNo++;
                String newLine = generateNewLine(currentNo, titleNoStack, line);
                lineArr[i] = newLine;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String line : lineArr) {
            stringBuilder.append(line).append("\n");
        }

        content = stringBuilder.toString();

        FileUtils.writeStringToFile(filePath, content, "UTF-8");
    }

    private static String generateNewLine(int currentNo, Stack<TitleInfo> titleNoStack, String line) {
        String currentTitlePrefix = generateTitlePrefix(line, titleNoStack, currentNo);
        String newLine = currentTitlePrefix + extractTitleContent(line);

        System.out.println("newLine=" + newLine);

        return newLine;
    }

    private static String generateTitlePrefix(String line, Stack<TitleInfo> titleNoStack, int currentNo) {
        if (!line.startsWith("#")) {
            throw new RuntimeException("当前行不是#开头，无法解析出#前缀！line=" + line);
        }

        Integer i;
        for (i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '#') {
                continue;
            } else {
                break;
            }
        }
        String hashSignPrefix = line.substring(0, i);

        StringBuilder prefixStringBuilder = new StringBuilder();
        prefixStringBuilder.append(hashSignPrefix);
        prefixStringBuilder.append(" ");

        for (TitleInfo titleInfo : titleNoStack) {
            prefixStringBuilder.append(titleInfo.getTitleNo()).append(".");
        }
        prefixStringBuilder.append(currentNo).append(".");
        prefixStringBuilder.append(" ");

        return prefixStringBuilder.toString();
    }

    private static boolean isTitle(String line) {
        if (StringUtils.isEmpty(line)) {
            return false;
        }
        if (line.startsWith("# ")
                || line.startsWith("## ")
                || line.startsWith("### ")
                || line.startsWith("#### ")
                || line.startsWith("##### ")
                || line.startsWith("###### ")) {
            return true;
        } else {
            return false;
        }
    }

    private static int getTitleLevel(String line) {
        if (StringUtils.isEmpty(line)) {
            throw new RuntimeException("当前行不是title，无法获取标题层级！line=" + line);
        }
        if (line.startsWith("# ")) {
            return 1;
        } else if (line.startsWith("## ")) {
            return 2;
        } else if (line.startsWith("### ")) {
            return 3;
        } else if (line.startsWith("#### ")) {
            return 4;
        } else if (line.startsWith("##### ")) {
            return 5;
        } else if (line.startsWith("###### ")) {
            return 6;
        } else {
            throw new RuntimeException("当前行无法获取标题层级！line=" + line);
        }
    }

    /**
     * 抽取标题内容
     *
     * 例如，
     *
     * ### 1.2.3. 旁路缓存
     * 对应的标题内容是 旁路缓存
     *
     * @param line
     * @return
     */
    private static String extractTitleContent(String line) {
        if (StringUtils.isEmpty(line)) {
            throw new RuntimeException("当前行不是title，无法获取标题内容！line=" + line);
        }

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if ('#'!= currentChar
                    && ' ' != currentChar
                    && (currentChar < '0' || currentChar > '9')
                    && '.' != currentChar) {
                String lineContent = line.substring(i);
                return lineContent;
            } else {
                continue;
            }
        }
        return "";
    }

}
