package com.shah.mql;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.Stack;

public class MQLUtility {

    final static String arraysAsList = "Arrays.asList";
    final static String newDocument = "new Document";

    public static String toMQL(String documentList) {
        if (!documentList.startsWith(arraysAsList))
            throw new AssertionError("Input parameter must start with " + arraysAsList);

        documentList = documentList.replaceAll("[\r\n]+", "");
        documentList = documentList.replaceAll("new BsonNull\\(\\)", "\"EMPTY\"");

        documentList = handelArrayListKeyword(documentList.toCharArray(), 0);
        documentList = handelCommasInsideArrayList(documentList.toCharArray());

        documentList = documentList.replaceAll(newDocument + "\\s+\\(", "{");
        documentList = documentList.replaceAll(newDocument + "\\(", "{");

        documentList = documentList.replaceAll("\\)", "}");
        documentList = documentList.replaceAll("#", ":");
        documentList = documentList.replaceAll("([0-9]+)(L+)(\\s*})", "$1 $3");

        documentList = documentList.replaceAll("\\{\\s+\"", "\\{");
        documentList = documentList.replaceAll("\\{\"", "\\{");

        documentList = documentList.replaceAll("\"\\s+:", ":");
        documentList = documentList.replaceAll("\":", ":");

        documentList = documentList.replaceAll("}\\s+.append\\(\"", ",");
        documentList = documentList.replaceAll("}.append\\s+\\(\"", ",");
        documentList = documentList.replaceAll("}.append\\(\\s+\"", ",");
        documentList = documentList.replaceAll("}.append\\(\"", ",");

        documentList = jsonStringToJsonFormat(documentList);
        documentList = documentList.replaceAll("\"EMPTY\"", "null");
        return documentList;
    }

    private static String handelCommasInsideArrayList(char[] toCharArray) {
        for (int k = 0; k < toCharArray.length; k++) {
            if (toCharArray[k] == ',') {
                for (int s = k; s >= 0; s--) {
                    if (toCharArray[s] == ')' || toCharArray[s] == '[') {
                        break;
                    } else if (toCharArray[s] == '(') {
                        toCharArray[k] = '#';
                    }
                }
            }
        }
        return new String(toCharArray);
    }

    private static String handelArrayListKeyword(char[] chars, int index) {
        Stack<Character> big = new Stack<>();
        String x = new String(chars);
        if (x.startsWith(arraysAsList, index)) {
            int first = 0;
            int last = 0;
            for (int i = index; i < chars.length; i++) {
                if (chars[i] == '(') {
                    if (first == 0) {
                        first = i;
                    }
                    big.push('(');
                }
                if (chars[i] == ')') {
                    big.pop();
                    if (big.size() == 0) {
                        last = i;
                        break;
                    }
                }
            }
            chars[first] = '[';
            chars[last] = ']';
            x = new String(chars);
            x = x.replaceFirst(arraysAsList, "");
            int i = x.indexOf(arraysAsList);
            if (i == -1) {
                return x;
            }
            return handelArrayListKeyword(x.toCharArray(), i);
        }
        return x;
    }

    private static String jsonStringToJsonFormat(String s) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(s);
        String prettyJsonString = gson.toJson(je);
        prettyJsonString = prettyJsonString.replaceAll("\"([$]\\w+)\":", "$1:");
        prettyJsonString = prettyJsonString.replaceAll("\"(\\w+)\":", "$1:");
        return prettyJsonString;
    }

    public static void main(String[] args) {
        String s = "Arrays.asList(\n" +
                "\t\t\t\tnew Document(\"$unwind\", new Document(\"path\", \"$financier\").append(\"preserveNullAndEmptyArrays\", true)),\n" +
                "\t\t\t\tnew Document(\"$group\", new Document(\"_id\", new BsonNull()).append(\"uniqueFinanciers\",\n" +
                "\t\t\t\t\t\tnew Document(\"$addToSet\", \"$financier\"))))";
        String prettyJsonString = toMQL(s);
        System.out.println(prettyJsonString);
    }
}
