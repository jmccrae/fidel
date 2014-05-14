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

import java.util.Arrays;
import java.util.List;

public interface PhraseTable {

    String getForeignLanguage();
    
    String getTranslationLanguage();
    
    Iterable<PhraseTableEntry> lookup(final List<String> terms);
    
    public static class PhraseTableEntry {
        private final Fidel.Label foreign;
        private final Fidel.Label translation;
        private final Fidel.Feature[] features;
        private final double appScore;

        public PhraseTableEntry(Fidel.Label foreign, Fidel.Label translation, Fidel.Feature[] features) {
            this.foreign = foreign;
            this.translation = translation;
            this.features = features;
            double as = 0.0;
            for(Fidel.Feature f : features) {
                as += f.score;
            }
            this.appScore = as;
                    
        }

        public Fidel.Label getForeign() {
            return foreign;
        }

        public Fidel.Label getTranslation() {
            return translation;
        }

        public Fidel.Feature[] getFeatures() {
            return features;
        }
        
        public double getApproxScore() {
            return appScore;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + (this.foreign != null ? this.foreign.hashCode() : 0);
            hash = 79 * hash + (this.translation != null ? this.translation.hashCode() : 0);
            hash = 79 * hash + Arrays.deepHashCode(this.features);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PhraseTableEntry other = (PhraseTableEntry) obj;
            if (this.foreign != other.foreign && (this.foreign == null || !this.foreign.equals(other.foreign))) {
                return false;
            }
            if (this.translation != other.translation && (this.translation == null || !this.translation.equals(other.translation))) {
                return false;
            }
            if (!Arrays.deepEquals(this.features, other.features)) {
                return false;
            }
            return true;
        }
    }
}
