package util;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class CsvWriter {
    private CsvWriter() {}
    public static void write(Path file,List<String[]> rows) throws IOException {
        Path parent=file.getParent(); if(parent!=null) Files.createDirectories(parent);
        try(BufferedWriter w=Files.newBufferedWriter(file,StandardCharsets.UTF_8)) {
            for(String[] row:rows) {
                for(int i=0;i<row.length;i++) { if(i>0) w.write(','); w.write(escape(row[i])); }
                w.newLine();
            }
        }
    }
    public static void write(String file,List<String[]> rows) throws IOException { write(Path.of(file),rows); }
    private static String escape(String s) {
        if(s==null) return ""; boolean q=s.contains(",")||s.contains("\"")||s.contains("\n")||s.contains("\r");
        String v=s.replace("\"","\"\""); return q?"\""+v+"\"":v;
    }
}
