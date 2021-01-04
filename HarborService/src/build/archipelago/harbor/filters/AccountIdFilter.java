package build.archipelago.harbor.filters;

import build.archipelago.account.common.AccountService;
import build.archipelago.common.exceptions.UnauthorizedException;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Component
@Slf4j
public class AccountIdFilter implements Filter {

    public static final String Key = "account-id";

    @Autowired
    private AWSCognitoIdentityProvider cognitoIdentityProvider;
    @Autowired
    private AccountService accountService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.info("Setting account id");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        List<String> headers = Collections.list(httpRequest.getHeaders("Authorization"));
        String authorization;
        if (headers.size() != 1) {
            chain.doFilter(request, response);
            return;
        }
        authorization = headers.get(0);

        String[] split = authorization.split(" ", 2);
        if (split.length != 2) {
            throw new UnauthorizedException("Invalid Authorization token");
        }
        String JWT = split[1];

        try {
            AdminGetUserResult result = cognitoIdentityProvider.adminGetUser(new AdminGetUserRequest().withUserPoolId("us-west-2_DWQyxBTOf").withUsername("test"));
            String userId = result.getUserAttributes().stream().filter(a -> "sub".equalsIgnoreCase(a.getName())).map(AttributeType::getValue).findFirst().get();
            String accountId = accountService.getAccountIdForUser(userId);
            request.setAttribute("account-id", accountId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        chain.doFilter(request, response);
    }
}
