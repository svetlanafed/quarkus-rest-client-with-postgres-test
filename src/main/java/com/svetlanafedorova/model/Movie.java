package com.svetlanafedorova.model;

import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Movie {

    private Long id;
    private String title;

    public Movie() {
    }

    public Movie(String title) {
        this.title = title;
    }

    public static Uni<List<Movie>> findAll(PgPool client) {
        return client.query("select id, title from movies order by title desc").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(Movie::from)
                .collect().asList();
    }

    public static Uni<Movie> findMovieById(PgPool client, Long id) {
        return client
                .preparedQuery("select id, title from movies where id=$1")
                .execute(Tuple.of(id))
                .onItem()
                .transform(m -> m.iterator().hasNext() ? from(m.iterator().next()) : null);
    }

    public static Uni<Long> save(Pool client, String title) {
        return client.
                preparedQuery("insert into movies (title) values ($1) returning id")
                .execute(Tuple.of(title))
                .onItem()
                .transform(m -> m.iterator().next().getLong("id"));
    }

    public static Uni<Boolean> delete(PgPool client, Long id) {
        return client.preparedQuery("delete from movies where id = $1")
                .execute(Tuple.of(id))
                .onItem()
                .transform(m -> m.rowCount() == 1);
    }

    private static Movie from(Row row) {
        return new Movie(row.getLong("id"), row.getString("title"));
    }
}
