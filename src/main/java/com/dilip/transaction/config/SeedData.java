package com.dilip.transaction.config;
import com.dilip.transaction.entity.Account; import com.dilip.transaction.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
@Configuration public class SeedData {
 @Bean CommandLineRunner seed(AccountRepository repo){return args->{for(int i=1;i<=5;i++){String id="ACC100"+i;if(!repo.existsById(id))repo.save(new Account(id));}};}
}