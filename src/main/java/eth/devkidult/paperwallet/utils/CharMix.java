package eth.devkidult.paperwallet.utils;

public class CharMix {


	public static String makeMessage(){
		String message = "";
		char [] basket = {'A','B','C','D','E','F','G','H','I','J','K','L'
				,'M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
				,'a','b','c','d','e','f','g','h','i','j','k','l'
				,'m','n','o','p','q','r','s','t','u','v','w','x','y','z'
				,'1','2','3','4','5','6','7','8','9'};

		for(int i = 0 ; i < 7 ; i++){
			int index = (int)(Math.random() * basket.length);
			message += basket[index];
		}
		return message;
	}
}