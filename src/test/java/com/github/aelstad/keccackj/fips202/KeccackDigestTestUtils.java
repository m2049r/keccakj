/*
 * Copyright 2014 Amund Elstad. 
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
package com.github.aelstad.keccackj.fips202;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.apache.commons.codec.binary.Hex;

import com.github.aelstad.keccackj.core.AbstractKeccackMessageDigest;
import com.github.aelstad.keccackj.core.KeccackSponge;

public class KeccackDigestTestUtils {

	public static class DigestTest {
		byte[] msg;
		int len;
		byte[] digest;
	}
	
	public List<DigestTest> parseTests(InputStream is) throws Exception 
	{
		List<DigestTest> rv = new ArrayList<DigestTest>();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		
		
		DigestTest nextTest = new DigestTest();
		while((line = br.readLine()) != null) {
			String token=null;
			String value=null;
			if(line.contains("=")) {
				token = line.split("=")[0].replace(" ", "");
				value = line.split("=")[1].replace(" ", "");
			}
			if(token != null && value != null && token.length() > 0 && value.length() > 0) {
				if(token.equalsIgnoreCase("len")) {
					nextTest.len = Integer.parseInt(value);
				} else if(token.equalsIgnoreCase("msg")) {
					nextTest.msg = Hex.decodeHex(value.toCharArray());
				} else if(token.equalsIgnoreCase("MD") || token.equalsIgnoreCase("Squeezed")) {
					nextTest.digest = Hex.decodeHex(value.toCharArray());
				}
				if(nextTest.len >= 0 && nextTest.msg != null && nextTest.digest != null) {
					rv.add(nextTest);
					nextTest = new DigestTest();
				}				
			}			
		}
		
		return rv;
	}
	
	public void runTests(List<DigestTest> tests, AbstractKeccackMessageDigest messageDigest, int digestLength)
		throws Exception
	{
		for(DigestTest dt : tests) {
			messageDigest.reset();
		
			if((dt.len & 7)==0)
				messageDigest.update(dt.msg, 0, dt.len>>3);
			else			
				messageDigest.engineUpdateBits(dt.msg, 0, dt.len);
			
			System.out.println("Rate is now "+ new String(Hex.encodeHex(messageDigest.getRateBits(0, Math.min(dt.len, messageDigest.getRateBits())))));
			byte[] md = messageDigest.digest();
			System.out.println("Testing length "+ dt.len + ". Got "+new String(Hex.encodeHex(md)));
			Assert.assertTrue(digestLength == dt.digest.length);;
			Assert.assertTrue(digestLength == md.length);;
			org.junit.Assert.assertTrue(Arrays.equals(md, dt.digest));			
		}
	}
	
	public void runTests(List<DigestTest> tests, KeccackSponge sponge)
	{
		for(DigestTest dt : tests) {
			sponge.reset();
		
			sponge.getAbsorbStream().writeBits(dt.msg, 0, dt.len);
			System.out.println("Rate is now "+ new String(Hex.encodeHex(sponge.getRateBits(0, Math.min(dt.len, sponge.getRateBits())))));
			byte[] rv = new byte[dt.digest.length];
			sponge.getSqueezeStream().read(rv);
			
			Assert.assertTrue(rv.length == dt.digest.length);;
			org.junit.Assert.assertTrue(Arrays.equals(rv, dt.digest));			
		}		
	}
}