package dao.interfaces;

import model.Post;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Created by wopqw on 05.11.16.
 */
public interface PostDAO {

    Collection<Post> getAll();

    default Collection<Post> getAllByUser(long userId){

        return getAll().stream()
                .filter(p -> p.getAuthorId() == userId)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
