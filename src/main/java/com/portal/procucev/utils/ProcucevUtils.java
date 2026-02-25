package com.portal.procucev.utils;

import java.util.Random;

public class ProcucevUtils {
	
	 public static char[] generatePassword(int length) {
	        String capitalCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
	        String specialCharacters = "!@#$";
	        String numbers = "1234567890";
	        String combinedChars = capitalCaseLetters + lowerCaseLetters + specialCharacters + numbers;

	        Random random = new Random();
	        char[] password = new char[length];

	        // First character: avoid special chars
	        String firstLastChars = capitalCaseLetters + lowerCaseLetters + numbers;
	        password[0] = firstLastChars.charAt(random.nextInt(firstLastChars.length()));

	        // Last character: avoid special chars
	        password[length - 1] = firstLastChars.charAt(random.nextInt(firstLastChars.length()));

	        // Ensure at least one lowercase, uppercase, number, special somewhere in the middle
	        password[1] = lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length()));
	        password[2] = capitalCaseLetters.charAt(random.nextInt(capitalCaseLetters.length()));
	        password[3] = specialCharacters.charAt(random.nextInt(specialCharacters.length()));
	        password[4] = numbers.charAt(random.nextInt(numbers.length()));

	        // Fill the remaining positions (excluding first and last) randomly
	        for (int i = 5; i < length - 1; i++) {
	            password[i] = combinedChars.charAt(random.nextInt(combinedChars.length()));
	        }

	        return password;
	    }


//	public static char[] generatePassword(int length) {
//		String capitalCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//		String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
//		String specialCharacters = "!@#$";
//		String numbers = "1234567890";
//		String combinedChars = capitalCaseLetters + lowerCaseLetters + specialCharacters + numbers;
//		Random random = new Random();
//		char[] password = new char[length];
//
//		password[0] = lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length()));
//		password[1] = capitalCaseLetters.charAt(random.nextInt(capitalCaseLetters.length()));
//		password[2] = specialCharacters.charAt(random.nextInt(specialCharacters.length()));
//		password[3] = numbers.charAt(random.nextInt(numbers.length()));
//
//		for (int i = 4; i < length; i++) {
//			password[i] = combinedChars.charAt(random.nextInt(combinedChars.length()));
//		}
//		return password;
//	}
}
