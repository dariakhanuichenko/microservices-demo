package deyadecember;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class CatalogApplication {
    public static void main(String[] args) {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(CatalogApplication.class, args);
    }

}
