package extensions;

import play.libs.Crypto;
import play.templates.JavaExtensions;

public class EncryptionExtensions extends JavaExtensions {
	
	public static String encrypt(Long data) {
	     return Crypto.encryptAES(data+"");
	}

}
