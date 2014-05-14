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
import static eu.monnetproject.translation.fidel.MurmurHash.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 *
 * @author John McCrae
 */
public class SolutionImpl implements Solution {

    public final int upto;
    public final int[] soln;
    public final int[] dist;
    public final double score;
    public final double futureCost;
    public final double[] features;

    public SolutionImpl(int upto, int[] soln, int[] dist, double score, double futureCost, double[] features) {
        assert (soln.length == dist.length);
        assert (futureCost >= score);
        this.upto = upto;
        this.soln = soln;
        this.dist = dist;
        this.score = score;
        this.futureCost = futureCost;
        this.features = features;
    }

    @Override
    public int compareTo(Solution t) {
        if (score < t.score()) {
            return +1;
        } else if (score > t.score()) {
            return -1;
        } else {
            if (t instanceof SolutionImpl) {
                SolutionImpl s = (SolutionImpl) t;
                if (upto < s.upto) {
                    return +1;
                } else if (upto > s.upto) {
                    return -1;
                } else {
                    if (soln.length < s.soln.length) {
                        return +1;
                    } else if (soln.length > s.soln.length) {
                        return -1;
                    }
                    for (int i = 0; i < soln.length; i++) {
                        final int c = soln[i] - s.soln[i];
                        if (c != 0) {
                            return c;
                        }
                    }
                    return 0;
                }
            } else {
                return -1;
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.upto;
        hash = 97 * hash + hash32(this.soln);
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
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
        final SolutionImpl other = (SolutionImpl) obj;
        if (this.upto != other.upto) {
            return false;
        }
        if (!Arrays.equals(this.soln, other.soln)) {
            return false;
        }
        if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Solution{" + "upto=" + upto + ", soln=" + Arrays.toString(soln) + ", dist=" + Arrays.toString(dist) + ", score=" + score + " (" + futureCost + ")}";
    }

    @Override
    public double score() {
        return score;
    }

    @Override
    public int[] soln() {
        return soln;
    }

    @Override
    public double[] features() {
        return features;
    }
    
    @Override
    public void printSoln(Int2ObjectMap<String> wordMap) {
        System.err.print("Solution: ");
        for (int i = 0; i < soln.length; i++) {
            if (wordMap != null) {
                System.err.print(wordMap.get(soln[i]) + "(" + dist[i] + ") ");
            } else {
                System.err.print(soln[i] + "(" + dist[i] + ") ");
            }
        }
        System.err.println("\t\t" + score + " - " + futureCost);
    }
}
