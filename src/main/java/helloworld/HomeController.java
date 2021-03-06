package helloworld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

	@Autowired
	private DataSource datasource;
	
    @Autowired
    JdbcTemplate jdbcTemplate;

	
    @RequestMapping("/")
    public String home(Model model) {

    	model.addAttribute(
    			"mysql_date", this.jdbcTemplate.queryForObject("SELECT SYSDATE() FROM DUAL", String.class)
    			);
   
    	model.addAttribute(
    			"mysql_url", toString(datasource)
    			);
    	
        return "home";
    }

    private String toString(DataSource dataSource) {
        if (dataSource == null) {
            return "<none>";
        } else {
            try {
                Field urlField = ReflectionUtils.findField(dataSource.getClass(), "url");
                ReflectionUtils.makeAccessible(urlField);
                return stripCredentials((String) urlField.get(dataSource));
            } catch (Exception fe) {
                try {
                    Method urlMethod = ReflectionUtils.findMethod(dataSource.getClass(), "getUrl");
                    ReflectionUtils.makeAccessible(urlMethod);
                    return stripCredentials((String) urlMethod.invoke(dataSource, (Object[])null));
                } catch (Exception me){
                    return "<unknown> " + dataSource.getClass();
                }
            }
        }
    }

    private String stripCredentials(String urlString) {
        try {
            if (urlString.startsWith("jdbc:")) {
                urlString = urlString.substring("jdbc:".length());
            }
            URI url = new URI(urlString);
            
           String[] query = url.getQuery().split("&");
           
           List<String> allowedPairs = new ArrayList<String>();
           allowedPairs.add("USESSL");
           allowedPairs.add("REQUIRESSL");
           allowedPairs.add("VERIFYUSERCERTIFICATE");
           
           String allowedQuery = "";
           for (String pair : query) {
        	   String[] kv = pair.split("=");
        	   if (allowedPairs.contains(kv[0].toUpperCase())) {
        		   if (allowedQuery.equals("")) {
        			   allowedQuery = kv[0] + "=" + kv[1];
        		   } else {
        			   allowedQuery = allowedQuery + "&" + kv[0] + "=" + kv[1];
        		   }
        	   }
           }
            
            return new URI(url.getScheme(), null, url.getHost(), url.getPort(), url.getPath(), allowedQuery, url.getFragment()).toString();
        }
        catch (URISyntaxException e) {
            System.out.println(e);
            return "<bad url> " + urlString;
        }
    }

}
