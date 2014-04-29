/* LanguageTool, a natural language style checker 
 * Copyright (C) 2014 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.tagging.de;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.WordData;
import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.tagging.TokenPoS;
import org.languagetool.tagging.ValueSet;
import org.languagetool.tools.StringTools;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GermanPosTagResolverTest {

  private final GermanPosTagResolver resolver = new GermanPosTagResolver();

  @Test
  public void test() {
    assertTag("SUB:AKK:SIN:FEM", "pos=nomen, kasus=akkusativ, numerus=singular, genus=femininum");
    assertTag("EIG:AKK:SIN:NEU:ART:STD", "pos=eigenname, kasus=akkusativ, numerus=singular, genus=neutrum, artikel=mit, eigenname=stadt");  // "Ülzen"
    assertTag("VER:1:SIN:KJ2:SFT:NEB", "pos=verb, person=1, numerus=singular, modus=konjunktiv2, konjugation=schwach, gebrauch=nebensatz");  // "abrodete"
    assertTag("VER:PA1:SFT", "pos=verb, form=partizip1, konjugation=schwach");  // "abrodend"
    assertTag("VER:INF:SFT", "pos=verb, form=infinitiv, konjugation=schwach");  // "abroden"
    assertTag("VER:IMP:SIN:SFT", "pos=verb, form=imperativ, numerus=singular, konjugation=schwach");  // "behänge"
    assertTag("ADJ:PRD:KOM", "pos=adjektiv, gebrauch=prädikativ, komparation=komparativ");  // "zotteliger"
    assertTag("ADJ:DAT:SIN:MAS:SUP:DEF", "pos=adjektiv, kasus=dativ, numerus=singular, genus=maskulinum, komparation=superlativ, art=bestimmt");  // "schärfsten"
    assertTag("ART:DEF:NOM:PLU:FEM", "pos=artikel, artikel=bestimmt, kasus=nominativ, numerus=plural, genus=femininum");  // "die"
    assertTag("PRO:RIN:DAT:FEM", "pos=pronomen, pronomen=interrogativ|relativ, kasus=dativ, genus=femininum");  // "wem"
    assertTag("PRO:RIN:GEN:SIN:NEU:B/S", "pos=pronomen, pronomen=interrogativ|relativ, kasus=genitiv, numerus=singular, genus=neutrum, stellung=begleitend|stellvertretend");  // "welches"
    assertTag("ADV:TMP", "pos=adverb, adverb=temporal");  // "zuweilen"
    assertTag("ADV:MOD+TMP+LOK", "pos=adverb, adverb=lokal|modal|temporal");  // "zusammen"
    assertTag("PRP:MOD:GEN+DAT", "pos=präposition, präposition=modal, kasus=dativ|genitiv");  // "zugunsten"
    assertTag("NEG", "pos=negationspartikel");  // "nein"
    assertTag("ABK", "pos=abkürzung");  // "evtl"
    assertTag("ZAL", "pos=zahlwort");  // "zwanzig"
    assertTag("INJ", "pos=interjektion");  // "naja"
    assertTag("ZUS", "pos=verbzusatz");  // "übrig"
  }

  @Test
  public void testDictionary() throws IOException {
    resolver.setStrictResolveMode(true);
    Dictionary dictionary = Dictionary.read(
            JLanguageTool.getDataBroker().getFromResourceDirAsUrl("/de/german.dict"));
    DictionaryLookup dl = new DictionaryLookup(dictionary);
    for (WordData wd : dl) {
      String posTag = wd.getTag().toString();
      if (ignoreKnownProblems(wd)) {
        System.err.println("Ignoring: " + posTag + " for word '" + wd.getWord() + "'");
        continue;
      }
      try {
        resolver.resolvePOSTag(posTag);
      } catch (Exception e) {
        //System.err.println("Could not resolve '" + posTag + "' for word '" + wd.getWord() + "': " + e.getMessage());
        throw new RuntimeException("Could not resolve '" + posTag + "' for word '" + wd.getWord() + "'", e);
      }
    }
  }

  /**
   * Known problems with the data, ignore for now.
   * TODO: remove once the data is fixed
   */ 
  private boolean ignoreKnownProblems(WordData wd) {
    String word = wd.getWord().toString();
    String pos = wd.getTag().toString();
    if ("Nummerierungen".equals(word)) {
      return true;
    }
    if ("höher".equals(word) && "ADJ:PRD".equals(pos)) {
      return true;
    }
    if (pos.contains("llemma") || pos.contains(":DAR:")) {
      return true;
    }
    return false;
  }

  private void assertTag(String input, String expected) {
    List<TokenPoS> pos = resolver.resolvePOSTag(input);
    String[] parts = expected.split(", ");
    assertThat(pos.size(), is(1));
    for (String part : parts) {
      String[] keyVal = part.split("=");
      String expectedKey = keyVal[0];
      String expectedVal = keyVal[1];
      ValueSet values = pos.get(0).getValues(expectedKey);
      if (values == null) {
        throw new RuntimeException("No value found for expected key '" + expectedKey + "'");
      }
      List<String> sortedValues = new ArrayList<>(values.getValues());
      Collections.sort(sortedValues, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          return o1.compareTo(o2);
        }
      });
      assertThat("Failure for input '" + input + "', key '" + expectedKey + "'", StringTools.listToString(sortedValues, "|"), is(expectedVal));
    }
  }
}