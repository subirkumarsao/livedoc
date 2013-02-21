package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Random;

import models.Document;
import models.User;
import models.oauthclient.Credentials;

import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.contenttype.ContentTypeManager;
import org.docx4j.openpackaging.contenttype.ContentTypes;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.VbaDataPart;
import org.docx4j.openpackaging.parts.WordprocessingML.VbaProjectBinaryPart;
import org.docx4j.utils.ResourceUtils;

import com.lowagie.text.Header;

import play.libs.Crypto;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.modules.oauthclient.ICredentials;
import play.mvc.Controller;
import play.vfs.VirtualFile;

public class Application extends Controller {
	
	public static Random random = new Random();
	
	public static void logout() throws Exception {
		session.remove("userId");
		index();
	}
	
    public static void index() throws Exception {
    	
    	String userId = session.get("userId");
    	
    	if(userId==null){
    		login();
    	}
    	Long id = Long.parseLong(userId);
    	User user = User.findById(id);
    	/*if(user.facebookId!=null && user.facebookId.equals("100000748839074")
    			&& session.get("dropbox_token")==null){
    		Dropbox.auth();
    	}*/
        home(null);
    }
    
    public static void home(String message) throws Exception {
    	
    	String userId = session.get("userId");
    	
    	if(userId==null){
    		login();
    	}
    	Long id = Long.parseLong(userId);
    	User user = User.findById(id);
    	render(user,message);
    }
    
    public static void login() throws Exception {
        render();
    }
    
    public static ICredentials getCredential(){
    	ICredentials creds = new Credentials();
    	creds.setSecret("n4gey29jvbi6oyo");
    	creds.setToken("ekorb6byf08e0ak");
    	return creds;
    }
    
    public static void isUpdateRequired(String docId,String docVersion){
    	Long id = Long.parseLong(Crypto.decryptAES(docId));
    	Document document =  Document.findById(id);
    	int version = Integer.parseInt(Crypto.decryptAES(docVersion));
    	if(document.version!=version){
    		renderText("true");
    	}
    	renderText("false");
    }
    
    public static void download(String docId,String type) throws Exception{
    	
    	Long id = Long.parseLong(Crypto.decryptAES(docId));
    	Document document =  Document.findById(id);
    	
    	if(type!=null && type.equals("pvt")){
    		type="Private";
    	}
    	else{
    		type="Public";
    	}
    	
    	String url = Dropbox.client.sign(getCredential(), "https://api-content.dropbox.com/1/files" +
    			"/dropbox/APP/"+type+"/"+document.id+".docm");
    	response.setHeader("Content-disposition", "attachment; filename="+document.name);
    	redirect(url);
    }
    public static void main(String[] args) {
		System.out.println(Crypto.encryptAES("1"));
	}
    public static void update(File file, String docId,String docVersion) throws Exception{
    	Long id = Long.parseLong(Crypto.decryptAES(docId));
    	Document document =  Document.findById(id);
    	document.version++;
    	
    	// Private File
    	
    	WS.WSRequest request =WS.url(Dropbox.client.sign(getCredential(), "https://api.dropbox.com/1/" +
    			"metadata/dropbox/APP/Private/"+document.id+".docm"));
    	HttpResponse response = request.get();
    	String rev = response.getJson().getAsJsonObject().get("rev").getAsString();
    	
    	String param = "parent_rev="+rev;
    	
    	File prvtFile = createPrivateDocument(file,document.id,(long)document.version);
    	
    	request = WS.url("https://api-content.dropbox.com/1/" +
    			"files_put/dropbox/APP/Private/"+document.id+".docm"+"?param="+URLEncoder.encode(param));
    	InputStream ios = new FileInputStream(prvtFile);
    	request.body(ios); 
    	response = Dropbox.client.sign(getCredential(),request ,"POST").post();
    	 
    	prvtFile.delete();
    	
    	// Public File
    	
    	request =WS.url(Dropbox.client.sign(getCredential(), "https://api.dropbox.com/1/" +
    			"metadata/dropbox/APP/Public/"+document.id+".docm"));
    	response = request.get();
    	rev = response.getJson().getAsJsonObject().get("rev").getAsString();
    	
    	param = "parent_rev="+rev;
    	
    	File pubFile = createPublicDocument(file,document.id,(long)document.version);
    	
    	request = WS.url("https://api-content.dropbox.com/1/" +
    			"files_put/dropbox/APP/Public/"+document.id+".docm"+"?param="+URLEncoder.encode(param));
    	ios = new FileInputStream(pubFile);
    	request.body(ios); 
    	Dropbox.client.sign(getCredential(),request ,"POST").post();
    	
    	pubFile.delete();
    	
    	document.save();
    	
    }

    public static void upload(File file) throws Exception{
    	
    	if(file.length()>(2*1024*1024)){
    		home("File length greater then 2 MB.");
    	}
    	if(!file.getName().split("\\.(?=[^\\.]+$)")[1].equalsIgnoreCase("docx")){
    		home("Invalid file format. It should be Word 2007 docx file.");
    	}
    	
    	Document document = new Document();
    	document.name = file.getName();
    	document.version = 1;
    	document.save();
    	
    	ICredentials creds = getCredential();
    	
    	File prvtFile = createPrivateDocument(file,document.id,(long)document.version);
    	{
	    	WSRequest request = WS.url("https://api-content.dropbox.com/1/" +
	    			"files_put/dropbox/APP/Private/"+document.id+".docm");
	    	InputStream ios = new FileInputStream(prvtFile);
	    	request.body(ios); 
	    	Dropbox.client.sign(creds,request ,"POST").post();
	    	ios.close();
	    	ios=null;
    	}
    	prvtFile.delete();
    	
    	File pubFile = createPublicDocument(file,document.id,(long)document.version);
    	{
    		WSRequest request = WS.url("https://api-content.dropbox.com/1/" +
    			"files_put/dropbox/APP/Public/"+document.id+".docm");
    		InputStream ios = new FileInputStream(pubFile);
    		request.body(ios); 
    		Dropbox.client.sign(creds,request ,"POST").post();
    		ios.close();
	    	ios=null;
    	}
    	pubFile.delete();
    	
    	String userId = session.get("userId");
    	Long id = Long.parseLong(userId);
    	User user = User.findById(id);
    	user.documents.add(document);
    	user.save();
    	
    	index();
    }
    public static File createPublicDocument(File file,Long docId,Long docVersion) throws Exception{
    	return createDocument(file, "files/public",docId,docVersion);
    }
    public static File createPrivateDocument(File file,Long docId,Long docVersion) throws Exception{
    	return createDocument(file, "files/private",docId,docVersion);
    }
    public static File createDocument(File file,String baseurl,Long docId, Long docVersion) throws Exception{
    	
    	WordprocessingMLPackage p = WordprocessingMLPackage.load(file); 
    	MainDocumentPart wordDocumentPart = p.getMainDocumentPart();

    	java.io.InputStream is = ResourceUtils.getResource(baseurl+"/vbaProject.bin");                  
    	VbaProjectBinaryPart vbaProject = new VbaProjectBinaryPart();
    	vbaProject.setBinaryData(is);
    	wordDocumentPart.addTargetPart(vbaProject);

    	// Get /word/vbaData.xml, and attach it to vbaProject
    	VbaDataPart vbaData = new VbaDataPart();
    	java.io.InputStream is2 = ResourceUtils.getResource(baseurl+"/vbaData.xml");                    
    	vbaData.setDocument( is2 );

    	vbaProject.addTargetPart( vbaData);
    	
    	// Adding custom properties.
    	
    	if(p.getDocPropsCustomPart()==null){
    		org.docx4j.openpackaging.parts.DocPropsCustomPart docPropsCustomPart = new org.docx4j.openpackaging.parts.DocPropsCustomPart();         
            java.io.InputStream instream = ResourceUtils.getResource("files/private/custom.xml" );         
            docPropsCustomPart.unmarshal(instream);         
            p.addTargetPart(docPropsCustomPart);
    	}
    	p.getDocPropsCustomPart().setProperty("DocId", Crypto.encryptAES(""+docId));
    	p.getDocPropsCustomPart().setProperty("DocVersion", Crypto.encryptAES(""+docVersion));
    	// Change the Word document's content type!
    	wordDocumentPart.setContentType( new ContentType(
    	        ContentTypes.WORDPROCESSINGML_DOCUMENT_MACROENABLED ) );
    	ContentTypeManager ctm = p.getContentTypeManager();
    	PartName partName = wordDocumentPart.getPartName();

    	ctm.removeContentType( partName  );
    	ctm.addOverrideContentType( new java.net.URI("/word/document.xml"), 
    	        ContentTypes.WORDPROCESSINGML_DOCUMENT_MACROENABLED);
    	
    	VirtualFile vrf = VirtualFile.fromRelativePath("/app/temp/"+random.nextInt()+".docm");
    	File retFile = vrf.getRealFile();
    	p.save(retFile);
    	return retFile;
    	
    }
}