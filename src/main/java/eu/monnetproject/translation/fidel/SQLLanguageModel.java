/**
 * *******************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *******************************************************************************
 */
package eu.monnetproject.translation.fidel;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author John McCrae
 */
public class SQLLanguageModel implements IntegerLanguageModel {

    private final Object2IntMap<String> wordMap;
    private final Int2ObjectMap<String> invWordMap;
    private final int order;
    private final Connection conn;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException x) {
            System.err.println("SQLite JDBC Drive not available");
        }
    }

    private SQLLanguageModel(Object2IntMap<String> wordMap, Int2ObjectMap<String> invWordMap, int order, File dbFile) {
        this.wordMap = wordMap;
        this.invWordMap = invWordMap;
        this.order = order;
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException x) {
            throw new RuntimeException("Could not connect to database", x);
        }
    }

    public double[] get(Phrase phrase) {
        try {
            final PreparedStatement statement = this.conn.prepareStatement("select score, backoff from language_model where ngram=?");
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(int i = phrase.l; i < phrase.l + phrase.n; i++) {
                if (!first) {
                    sb.append(" ");
                }
                sb.append(phrase.p[i]);
                first = false;
            }
            statement.setString(1, sb.toString());
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                final double score = rs.getDouble("score");
                final double backoff = rs.getDouble("backoff");
                statement.close();
                if (backoff == 0.0) {
                    return new double[]{score};
                } else {
                    return new double[]{score, backoff};
                }
            } else {
                statement.close();
                return null;
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException x) {
            x.printStackTrace();
        }
    }

    public int order() {
        return order;
    }

    public Object2IntMap<String> wordMap() {
        return wordMap;
    }

    public Int2ObjectMap<String> invWordMap() {
        return invWordMap;
    }

    public static class Factory implements LanguageModelFactory {

        private final Object2IntMap<String> wordMap = new Object2IntRBTreeMap<String>();
        private final Int2ObjectMap<String> invWordMap = new Int2ObjectRBTreeMap<String>();
        private int n = 0;

        public SQLLanguageModel getModel(File file) {

            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException x) {
                System.err.println("SQLite JDBC Drive not available");
            }
            if (file.getPath().endsWith(".db")) {
                try {
                    init(file);
                } catch (SQLException x) {
                    throw new RuntimeException(x);
                }
                return new SQLLanguageModel(wordMap, invWordMap, n, file);
            } else {
                try {
                    final File dbFile = loadDB(file);
                    return new SQLLanguageModel(wordMap, invWordMap, n, dbFile);
                } catch (IOException x) {
                    throw new RuntimeException(x);
                } catch (SQLException x) {
                    throw new RuntimeException(x);
                }
            }
        }

        private File loadDB(File file) throws IOException, SQLException {
            final File dbFile;
            final Scanner in;
            if (file.getPath().endsWith(".gz")) {
                dbFile = new File(file.getPath().substring(0, file.getPath().length() - 3) + ".db");
                in = new Scanner(new GZIPInputStream(new FileInputStream(file)));
            } else {
                dbFile = new File(file.getPath() + ".db");
                in = new Scanner(file);
            }
            if (dbFile.exists()) {
                System.err.println(String.format("Reusing database %s. Delete this file if out of date.", dbFile.getPath()));
                init(dbFile);
                return dbFile;
            } else {
                System.err.println("Creating new database for language model");
            }
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException x) {
                throw new RuntimeException("SQLite JDBC Drive not available", x);
            }
            final Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            conn.setAutoCommit(false);
            final Statement stat = conn.createStatement();
            stat.execute("create table language_model (ngram text, score real, backoff real);");
            stat.execute("create index language_model_ngram on [language_model] (ngram)");
            stat.execute("create table word_map (word text, id integer)");
            stat.execute("create table ngram_order (value integer)");
            final PreparedStatement insert = conn.prepareStatement("insert into language_model values (?,?,?)");
            final PreparedStatement wordMapInsert = conn.prepareStatement("insert into word_map values (?,?)");
            int i = 0;
            int w = 0;
            while (in.hasNextLine()) {
                final String[] elems = in.nextLine().split("\t");
                if (elems.length == 2 || elems.length == 3) {
                    final StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    final String[] words = elems[1].split(" ");
                    for (String word : words) {
                        if (!first) {
                            sb.append(" ");
                        }
                        if (wordMap.containsKey(word)) {
                            sb.append(wordMap.getInt(word));
                        } else {
                            w++;
                            wordMap.put(word, w);
                            invWordMap.put(w, word);
                            wordMapInsert.setString(1, word);
                            wordMapInsert.setInt(2, w);
                            wordMapInsert.execute();
                            sb.append(w);
                        }
                        first = false;
                    }
                    n = Math.max(n, words.length);
                    insert.setString(1, sb.toString());
                    insert.setDouble(2, Double.parseDouble(elems[0]));
                    insert.setDouble(3, elems.length == 3 ? Double.parseDouble(elems[2]) : 0.0);
                    insert.execute();
                } // else { ignore }
                if (i++ % 1000000 == 999999) {
                    System.err.println(String.format("Line %d", i));
                }
            }
            stat.execute("insert into ngram_order values (" + n + ")");
            conn.commit();
            conn.setAutoCommit(true);
            stat.close();
            insert.close();
            conn.close();
            return dbFile;
        }

        private void init(File file) throws SQLException {
            final Connection conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            final Statement stat = conn.createStatement();
            final ResultSet rs1 = stat.executeQuery("select value from ngram_order");
            if (!rs1.next()) {
                throw new RuntimeException("Language model database corrupt, please delete");
            }
            n = rs1.getInt(1);
            final ResultSet rs2 = stat.executeQuery("select * from word_map");
            while (rs2.next()) {
                final String word = rs2.getString("word");
                final int id = rs2.getInt("id");
                wordMap.put(word, id);
                invWordMap.put(id, word);
            }
            stat.close();
            conn.close();
        }
    }
}
