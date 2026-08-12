package com.example.mcp.resource;

import com.example.mcp.entity.Product;
import io.fabric8.openshift.api.model.ProjectList;
import io.fabric8.openshift.client.OpenShiftClient;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @Inject
    OpenShiftClient openShiftClient;

    @GET
    public List<Product> listAll() {


        String ns = openShiftClient.config().getNamespace();

       ProjectList nslist =openShiftClient.projects().list();



        return Product.listAll();
    }

    @POST
    @Transactional
    public Product create(Product product) {
        product.persist();
        return product;
    }
}
