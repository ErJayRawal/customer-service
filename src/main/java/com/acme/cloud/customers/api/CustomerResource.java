package com.acme.cloud.customers.api;

import com.acme.cloud.customers.dto.CustomerProfileRequest;
import com.acme.cloud.customers.dto.CustomerProfileResponse;
import com.acme.cloud.customers.service.CustomerProfileService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/v1/customers")
@Authenticated
public class CustomerResource {

    @Inject
    CustomerProfileService customerProfileService;

    @Inject
    SecurityIdentity identity;

    @Inject
    UriInfo uriInfo;

    @ConfigProperty(name = "customer.identity.issuer")
    String issuer;

    @GET
    @Path("/me")
    public CustomerProfileResponse getMyProfile() {
        return customerProfileService.getProfile(issuer, identity.getPrincipal().getName());
    }

    @PUT
    @Path("/me")
    public Response putMyProfile(@Valid CustomerProfileRequest request) {
        CustomerProfileService.ProfileUpdateResult result =
                customerProfileService.updateProfile(issuer, identity.getPrincipal().getName(), request);
        Response.ResponseBuilder response = Response.status(
                result.created() ? Response.Status.CREATED : Response.Status.OK
        ).entity(result.profile());
        if (result.created()) {
            response.location(uriInfo.getAbsolutePath());
        }
        return response.build();
    }
}
