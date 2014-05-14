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

import it.unimi.dsi.fastutil.doubles.DoubleRBTreeSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author John McCrae
 */
public class Fidel {

    private final IntegerLanguageModel languageModel;
    private final Object2IntMap<String> srcWordMap, trgWordMap;
    private final Int2ObjectMap<String> srcInvMap, invWordMap;
    private final int distortionLimit = Integer.parseInt(System.getProperty("distortionlimit", "5"));
    private final Properties weights;
    private static final String[] DEFAULT_FEATURE_NAMES = {
        "phi(t|f)",
        "lex(t|f)",
        "phi(f|t)",
        "lex(f|t)",
        "phrasePenalty"
    };

    public Fidel(IntegerLanguageModel languageModel, Properties weights) {
        this.languageModel = languageModel;
        this.srcWordMap = Object2IntMaps.synchronize(new Object2IntOpenHashMap<String>());
        this.srcInvMap = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<String>());
        this.trgWordMap = languageModel.wordMap();
        this.invWordMap = languageModel.invWordMap();
        this.weights = weights;
    }

    public List<Translation> decode(List<String> phrase, PhraseTable phraseTable, String[] featureNames, int nBest) {
        return decode(phrase, phraseTable, featureNames, nBest, 50, false);
    }

    public List<Translation> decodeFast(List<String> phrase, PhraseTable phraseTable, String[] featureNames, int nBest) {
        return decode(phrase, phraseTable, featureNames, nBest, 20, true);
    }

    private List<Translation> decode(List<String> phrase, PhraseTable phraseTable, String[] featureNames, int nBest, int beamSize, boolean useLazy) {
        FidelDecoder.wordMap = invWordMap;
        FidelDecoder.srcWordMap = srcWordMap;
        // Convert the phrase to an integer array
        int[] src = convertPhrase(phrase, srcWordMap, srcInvMap);
        // Collect translation candidates
        Object2ObjectMap<Phrase, Collection<PhraseTranslation>> pt = convertPT(phraseTable.lookup(phrase), featureNames, beamSize + 10);
        // Extract paramaters
        int lmN = languageModel.order();
        double[] wts = new double[featureNames.length + FidelDecoder.PT];
        int i = FidelDecoder.PT;
        wts[FidelDecoder.UNK] = weights.getProperty("UnknownWord") != null ? Double.parseDouble(weights.getProperty("UnknownWord")) : -100.0;
        wts[FidelDecoder.DIST] = weights.getProperty("LinearDistortion") != null ? Double.parseDouble(weights.getProperty("LinearDistortion")) : 0.0;
        wts[FidelDecoder.LM] = weights.getProperty("LM") != null ? Double.parseDouble(weights.getProperty("LM")) : 0.0;
        for (String feat : featureNames) {
            wts[i++] = weights.getProperty(feat) != null ? Double.parseDouble(weights.getProperty(feat))
                    : (weights.getProperty("TM:" + feat) != null ? Double.parseDouble(weights.getProperty("TM:" + feat)) : 0);
        }
        // Do decoding
        final Solution[] translations = FidelDecoder.decode(src, pt, languageModel, lmN, wts, distortionLimit, nBest, beamSize, useLazy);
        
        // Convert translations to strings
        final StringBuilder sb = new StringBuilder();
        for (String w : phrase) {
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(w);
        }
        return convertTranslations(translations, new Label(sb.toString(), phraseTable.getForeignLanguage()), phraseTable.getTranslationLanguage(), featureNames);
    }

    private int[] convertPhrase(List<String> phrase, Object2IntMap<String> wordMap, Int2ObjectMap<String> invMap) {
        final int[] p = new int[phrase.size()];
        int i = 0;
        int W = wordMap.size();
        for (String s : phrase) {
            if (wordMap.containsKey(s)) {
                p[i++] = wordMap.getInt(s);
            } else {
                p[i++] = ++W;
                wordMap.put(s, W);
                invMap.put(W, s);
            }
        }
        return p;
    }

    private Phrase convertPhrase(String[] phrase, Object2IntMap<String> dict, Int2ObjectMap<String> invMap) {
        final int[] p = new int[phrase.length];
        int i = 0;
        int W = dict.size();
        for (String s : phrase) {
            if (dict.containsKey(s)) {
                p[i++] = dict.getInt(s);
            } else {
                p[i++] = ++W;
                dict.put(s, W);
                invMap.put(W, s);
            }
        }
        return new Phrase(p);
    }

    private Object2ObjectMap<Phrase, Collection<PhraseTranslation>> convertPT(Iterable<PhraseTable.PhraseTableEntry> phraseTable, String[] featureNames, int maxSize) {
        final Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>> pt = new Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>>();
        final Object2ObjectOpenHashMap<Phrase, DoubleRBTreeSet> approxScores = new Object2ObjectOpenHashMap<Phrase, DoubleRBTreeSet>();
        for (PhraseTable.PhraseTableEntry pte : phraseTable) {
            final Phrase src;// = convertPhrase(FairlyGoodTokenizer.split(pte.getForeign().asString()), srcWordMap);
            final Phrase trg;// = convertPhrase(FairlyGoodTokenizer.split(pte.getTranslation().asString()), trgDict);

            src = convertPhrase(FairlyGoodTokenizer.split(pte.getForeign().asString()), srcWordMap, srcInvMap);
            if (maxSize > 0) {
                if (!approxScores.containsKey(src)) {
                    approxScores.put(src, new DoubleRBTreeSet());
                }
                final DoubleRBTreeSet as = approxScores.get(src);
                if (as.size() >= maxSize) {
                    if (as.firstDouble() > pte.getApproxScore()) {
                        continue;
                    } else {
                        as.remove(as.firstDouble());
                        as.add(pte.getApproxScore());
                    }
                } else {
                    as.add(pte.getApproxScore());
                }
            }
            trg = convertPhrase(FairlyGoodTokenizer.split(pte.getTranslation().asString()), trgWordMap, invWordMap);
            final double[] wts = convertWeights(pte.getFeatures(), featureNames);
            final PhraseTranslation translation = new PhraseTranslation(trg.p, wts);
            if (!pt.containsKey(src)) {
                pt.put(src, new ArrayList<PhraseTranslation>());
            }
            pt.get(src).add(translation);
        }
        return pt;
    }

    private List<Translation> convertTranslations(Solution[] translations, Label srcLabel, String trgLang, String[] featureNames) {
        final ArrayList<Translation> converted = new ArrayList<Translation>();
        for (Solution soln : translations) {
            Feature[] features = new Feature[FidelDecoder.PT + featureNames.length];
            final double[] solnFeatures = soln.features();
            features[FidelDecoder.UNK] = new Feature("UnknownWord", solnFeatures[FidelDecoder.UNK]);
            features[FidelDecoder.DIST] = new Feature("LinearDistortion", solnFeatures[FidelDecoder.DIST]);
            features[FidelDecoder.LM] = new Feature("LM", solnFeatures[FidelDecoder.LM]);
            int i = FidelDecoder.PT;
            for (String featName : featureNames) {
                features[i] = new Feature(featName.startsWith("TM:") ? featName : ("TM:" + featName), solnFeatures[i]);
                i++;
            }
            converted.add(new Translation(soln, srcLabel, trgLang, invWordMap, srcInvMap, features));
        }
        return converted;
    }

    private double[] convertWeights(Feature[] features, String[] featureNames) {
        double[] wts = new double[featureNames.length];
        int i = 0;
        for (String featureName : featureNames) {
            for (Feature feat : features) {
                if (feat.name.equals(featureName)) {
                    wts[i] += feat.score;
                }
            }
            i++;
        }
        return wts;
    }

    public static void main(String[] args) throws Exception {
        final CommandLineParser parser = new BasicParser();
        final Options opts = new Options();
        opts.addOption("p", true, "The phrase table");
        opts.addOption("l", true, "The language model");
        opts.addOption("n", true, "Output n-best translations");
        opts.addOption("w", true, "The weights file");
        opts.addOption("f", true, "The foreign (source) language");
        opts.addOption("t", true, "The translation (target) language");
        opts.addOption("b", true, "The beam size (default=50)");
        opts.addOption("z", false, "Use lazy distortion");
        opts.addOption("v", false, "Display debugging information");
        opts.addOption("s", false, "Output scores");
        opts.addOption("?", false, "Display this message");
        final HelpFormatter help = new HelpFormatter();
        final String fidelHeader = "FIDEL: A Simple Decoder for Machine Translation";
        final String exampleUsage = "\nexample: fidel -p phrase-table.gz -l europarl.arpa.gz -w moses.ini < data";
        final CommandLine cli;
        try {
            cli = parser.parse(opts, args);
        } catch (ParseException x) {
            System.err.println(x.getMessage());
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        if (cli.hasOption("?")) {
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        if (!cli.hasOption("p")) {
            System.err.println("The phrase table is required");
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        if (!cli.hasOption("l")) {
            System.err.println("The language model is required");
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        if (!cli.hasOption("w")) {
            System.err.println("The weights are required");
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        if(cli.hasOption("v")) {
            System.setProperty("fidel.verbose", "true");
        }
        final int beamSize;
        if(cli.hasOption("b")) {
            try {
                beamSize = Integer.parseInt(cli.getOptionValue("b"));
            } catch(NumberFormatException x) {
                System.err.println("Beam size must be an integer");
                help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
                return;
            }
        } else {
            beamSize = 50;
        }
        final boolean useLazy = cli.hasOption("z");
        final String foreignLanguage;
        if (cli.hasOption("f")) {
            foreignLanguage = cli.getOptionValue("f");
        } else {
            foreignLanguage = "fr";
        }
        final String targetLanguage;
        if (cli.hasOption("t")) {
            targetLanguage = cli.getOptionValue("t");
        } else {
            targetLanguage = "en";
        }
        final boolean outputScores = cli.hasOption("s");
        final Properties weights = new Properties();
        try {
            weights.load(new FileReader(cli.getOptionValue("w")));
        } catch(Exception x) {
            System.err.println(String.format("Could not access %s", cli.getOptionValue("w")));
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        final int n;
        try {
            n = Integer.parseInt(cli.getOptionValue("n", "1"));
        } catch (NumberFormatException x) {
            System.err.println("n must be an integer");
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        final File phraseTableFile = new File(cli.getOptionValue("p"));
        final File languageModelFile = new File(cli.getOptionValue("l"));
        if(!phraseTableFile.exists()) {
            System.err.println(String.format("%s does not exist", phraseTableFile.getPath()));
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        if(!languageModelFile.exists()) {
            System.err.println(String.format("%s does not exist", languageModelFile.getPath()));
            help.printHelp("fidel [opts]", fidelHeader, opts, exampleUsage);
            return;
        }
        
        final IntegerLanguageModel languageModel = new SQLLanguageModel.Factory().getModel(languageModelFile);
        final PhraseTable phraseTable = new SQLPhraseTable.Factory().getPhraseTable(foreignLanguage, targetLanguage,DEFAULT_FEATURE_NAMES , phraseTableFile);
        try {
            final Fidel fidel = new Fidel(languageModel, weights);
            final Scanner in = new Scanner(System.in);
            System.err.println("Translation system initialized");
            while(in.hasNextLine()) {
                final String line = in.nextLine();
                final List<Translation> translations = fidel.decode(Arrays.asList(FairlyGoodTokenizer.split(line)), phraseTable, DEFAULT_FEATURE_NAMES, n, beamSize, useLazy);
                for(Translation t : translations) {
                    if(outputScores) {
                        System.out.println(t);  
                    } else {
                        System.out.println(t.getTargetLabel());
                    }
                }
            }
        } finally {
            languageModel.close();
            phraseTable.close();
        }

    }

    public static class Translation {

        final Solution solution;
        final Label srcLabel;
        final Label trgLabel;
        final Feature[] features;

        public Translation(Solution solution, Label srcLabel, String trgLang, Int2ObjectMap<String> invMap, Int2ObjectMap<String> srcInvMap, Feature[] features) {
            this.solution = solution;
            this.srcLabel = srcLabel;
            this.features = features;
            StringBuilder sb = new StringBuilder();
            for (int w : solution.soln()) {
                if (sb.length() != 0) {
                    sb.append(" ");
                }
                if (w >= 0) {
                    sb.append(invMap.get(w));
                } else {
                    sb.append(srcInvMap.get(-w));
                }
            }
            this.trgLabel = new Label(sb.toString(), trgLang);
        }

        public Label getSourceLabel() {
            return srcLabel;
        }

        public Label getTargetLabel() {
            return trgLabel;
        }

        public double getScore() {
            return solution.score();
        }

        @SuppressWarnings("unchecked")
        public Collection<Feature> getFeatures() {
            return Arrays.asList(features);
        }

        @Override
        public String toString() {
            return String.format("%s [%.6f from %s]", trgLabel, solution.score(), srcLabel);
        }
        
        
    }

    public static class Label {

        private final String label;
        private final String language;

        public Label(String label, String language) {
            this.label = label;
            this.language = language;
        }

        public String asString() {
            return label;
        }

        public String getLanguage() {
            return language;
        }

        @Override
        public String toString() {
            return label;
        }
        
    }

    /**
     * A feature score for a single translation
     *
     * @author John McCrae
     */
    public static final class Feature {

        /**
         * The name of the feature
         */
        public final String name;
        /**
         * The score of the feature
         */
        public final double score;

        public Feature(String name, double score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Feature other = (Feature) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 53 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
            return hash;
        }

        @Override
        public String toString() {
            return name + "=" + score;
        }
    }
}
