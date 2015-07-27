/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.scribe.model.*;

/**
 *
 * @author Hamza
 */
@ManagedBean(name = "auth")
@SessionScoped
public class AuthenticationBean {

    public OAuthService service;
    public Token requestToken;
    public String userInfo;
    public boolean login;

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public boolean isLogin() {
        return login;
    }

    public void setLogin(boolean login) {
        this.login = login;
    }

    public void userLogin() {
        service = new ServiceBuilder()
                .provider(LinkedInApi.class)
                .apiKey("İstemci Kimliği(ApiKey) giriniz")
                .apiSecret("İstemci Gizliliği(ApiSecret) giriniz")
                //Eğer callback urli silerseniz verify kodunu elinizle girersiniz.Parametreyi Url den almamız gerekmez.
                //Linkedin verdiğimiz callback urline get metoduyla bize verifier kodunu parametre olarak gönderir
                .callback("http://localhost:8080/LinkedinAuthentication/redirectpage.xhtml") 
                .build();

        System.out.println(
                "Fetching the Request Token...");
        requestToken = service.getRequestToken();

        System.out.println(service.getAuthorizationUrl(requestToken));

        String url = service.getAuthorizationUrl(requestToken);

        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(url);
        } catch (IOException ex) {
            Logger.getLogger(AuthenticationBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*Kullanıcı Linkedinle giriş yapıp izin verdikten sonra linkedin kullanıcıyı
     bizim callback te verdiğimiz adrese yönlendirir*/
    public void authenticate() throws InterruptedException {
        System.out.println("\n-----------Parametreyi Al------------\n");
        HttpServletRequest origRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String paramater = origRequest.getParameter("oauth_verifier"); // Verifier parametresi

        System.out.println(paramater);
        Verifier verifier = new Verifier(paramater);

        System.out.println("\n-----------Acses Token elde et------------\n");
        Token accessToken = service.getAccessToken(requestToken, verifier);

        System.out.println("\n-----------İstek Gönder------------\n");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://api.linkedin.com/v1/people/~:(id,first-name,last-name,picture-urls::(original),skills,location,positions,email-address,industry)");
        stringBuilder.append("?oauth2_access_token=");
        stringBuilder.append(accessToken.getToken());
        stringBuilder.append("&format=json");
        OAuthRequest request = new OAuthRequest(Verb.GET, stringBuilder.toString());
        service.signRequest(accessToken, request);
        Response response = request.send();
        System.out.println("Sonuc\n\n\n" + response.getBody());
        //response.getBody den dönen String json formatındadır bunu json parse ile parse edip kullanabilirsiniz.
        userInfo = response.getBody();
        login = true;
        //İşlem bittikten sonra anaSayfaya yönlendiriyoruz
        try {

            FacesContext.getCurrentInstance().getExternalContext().redirect("index.xhtml");
        } catch (IOException ex) {
            Logger.getLogger(AuthenticationBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
