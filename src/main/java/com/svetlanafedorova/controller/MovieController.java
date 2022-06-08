package com.svetlanafedorova.controller;

import java.net.URI;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.svetlanafedorova.model.Movie;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;


@Path("/movies")
public class MovieController {

    @Inject
    PgPool client;

    @PostConstruct
    void config() {
        initDb();
    }

    private void initDb() {
        client.query("drop table if exists movies").execute()
                .flatMap(m -> client.query("create table movies (id serial primary key, title text not null)")
                        .execute())
                .flatMap(m -> client.query("insert into movies (title) values ('The Lord of the Rings')").execute())
                .flatMap(m -> client.query("insert into movies (title) values ('Harry Potter')").execute())
                .subscribeAsCompletionStage();
    }

    @GET
    public Uni<List<Movie>> get() {
        return Movie.findAll(client);
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getById(@PathParam("id") Long id) {
        return Movie.findMovieById(client, id)
                .onItem()
                .transform(movie -> movie != null ? Response.ok(movie) : Response.status(Status.NOT_FOUND))
                .onItem()
                .transform(Response.ResponseBuilder::build);
    }

    @POST
    public Uni<Response> create(Movie movie) {
        return Movie.save(client, movie.getTitle())
                .onItem()
                .transform(id -> URI.create("/movies/" + id))
                .onItem()
                .transform(uri -> Response.created(uri).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") Long id) {
        return Movie.delete(client, id)
                .onItem()
                .transform(deleted -> Boolean.TRUE.equals(deleted) ? Status.NO_CONTENT : Status.NOT_FOUND)
                .onItem()
                .transform(status -> Response.status(status).build());
    }
}
