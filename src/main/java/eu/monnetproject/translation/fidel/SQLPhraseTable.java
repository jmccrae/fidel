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

import eu.monnetproject.translation.fidel.Fidel.Feature;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author John McCrae
 */
public class SQLPhraseTable implements PhraseTable {

    private final String foreignLanguage, translationLanguage;
    private final String[] features;
    private final Connection conn;
    private static final boolean verbose = Boolean.parseBoolean(System.getProperty("fidel.verbose", "false"));

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException x) {
            System.err.println("SQLite JDBC Drive not available");
        }
    }

    private SQLPhraseTable(String foreignLanguage, String translationLanguage, String[] features, File dbFile) {
        this.foreignLanguage = foreignLanguage;
        this.translationLanguage = translationLanguage;
        this.features = features;
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException x) {
            throw new RuntimeException("Could not connect to database", x);
        }
    }

    public String getForeignLanguage() {
        return foreignLanguage;
    }

    public String getTranslationLanguage() {
        return translationLanguage;
    }

    public void close() {
        try {
            conn.close();
        } catch(SQLException x) {
            x.printStackTrace();
        }
    }
    
    

    private String mkString(List<String> terms, int i, int j) {
        boolean first = true;
        final StringBuilder sb = new StringBuilder();
        for (int k = i; k < j; k++) {
            if (!first) {
                sb.append(" ");
            }
            sb.append(terms.get(k));
            first = false;
        }
        return sb.toString();
    }

    private PhraseTableEntry mkPTE(ResultSet rs, String source) throws SQLException {

        final String translation = rs.getString("translation");
        final String[] scores = rs.getString("scores").split(" ");
        final Feature[] fs = new Feature[scores.length];
        for (int i = 0; i < scores.length; i++) {
            fs[i] = new Feature(features[i], Math.log10(Double.parseDouble(scores[i])));
        }
        return new PhraseTableEntry(new Fidel.Label(source, foreignLanguage), new Fidel.Label(translation, translationLanguage), fs);
    }

    public Iterable<PhraseTableEntry> lookup(List<String> terms) {

        try {
            final PreparedStatement stat = conn.prepareStatement("select translation, scores from phrase_table where forin=?");
            final ArrayList<PhraseTableEntry> result = new ArrayList<PhraseTableEntry>();
            for (int i = 0; i < terms.size(); i++) {
                for (int j = i + 1; j <= terms.size(); j++) {
                    final String query = mkString(terms, i, j);
                    stat.setString(1, query);
                    final ResultSet rs = stat.executeQuery();
                    while (rs.next()) {
                        result.add(mkPTE(rs, query));
                    }
                }
            }
            if(verbose) {
                System.err.println(String.format("Collected %d translation candidates", result.size()));
            }
            return result;
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    public static class Factory implements PhraseTableFactory {

        public SQLPhraseTable getPhraseTable(String foreignLanguage, String translationLanguage, String[] featureNames, File file) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException x) {
                System.err.println("SQLite JDBC Drive not available");
            }
            if (file.getName().endsWith(".db")) {
                return new SQLPhraseTable(foreignLanguage, translationLanguage, featureNames, file);
            } else {
                try {
                    return new SQLPhraseTable(foreignLanguage, translationLanguage, featureNames, loadDB(file));
                } catch (IOException x) {
                    throw new RuntimeException(x);
                } catch (SQLException x) {
                    throw new RuntimeException(x);
                }
            }
        }

        private File loadDB(File phraseTable) throws IOException, SQLException {
            final File dbFile;
            final Scanner in;
            if (phraseTable.getPath().endsWith(".gz")) {
                dbFile = new File(phraseTable.getPath().substring(0, phraseTable.getPath().length() - 3) + ".db");
                in = new Scanner(new GZIPInputStream(new FileInputStream(phraseTable)));
            } else {
                dbFile = new File(phraseTable.getPath() + ".db");
                in = new Scanner(phraseTable);
            }
            if (dbFile.exists()) {
                System.err.println(String.format("Reusing database %s. Delete this file if out of date.", dbFile.getPath()));
                return dbFile;
            } else {
                System.err.println("Creating new database for phrase table");
            }
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException x) {
                throw new RuntimeException("SQLite JDBC Drive not available", x);
            }
            final Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            conn.setAutoCommit(false);
            final Statement stat = conn.createStatement();
            // "foreign" is an SQL keyword, "forin" is creative spelling ;)
            stat.execute("create table phrase_table (forin text, translation text, scores text);");
            stat.execute("create index phrase_table_foreign on [phrase_table] (forin)");
            final PreparedStatement insert = conn.prepareStatement("insert into phrase_table values (?,?,?)");
            int i = 0;
            while (in.hasNextLine()) {
                final String[] elems = in.nextLine().split(" \\|\\|\\| ");
                insert.setString(1, elems[0]);
                insert.setString(2, elems[1]);
                insert.setString(3, elems[2]);
                insert.execute();
                if (i++ % 1000000 == 999999) {
                    System.err.println(String.format("Line %d", i));
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
            stat.close();
            insert.close();
            conn.close();
            return dbFile;
        }
    }
}
