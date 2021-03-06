package webapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import common.JsonWrapper;
import dao.*;
import listeners.Initer;
import lombok.extern.slf4j.Slf4j;
import model.Post;
import model.PostView;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * Created by wopqw on 05.11.16.
 */

@Slf4j
@Path("/posts/")
public class PostResource {

    private static PostDAO postDAO;
    private static UserDAO userDAO;
    private static FollowingDAO followingDAO;
    private static LikeDAO likeDAO;
    private static CommentDAO commentDAO;

    @Context
    public void init(ServletContext servletContext) {

        if (postDAO == null) {
            postDAO = (PostDAO) servletContext.getAttribute(Initer.POST_DAO);
        }
        if (userDAO == null) {
            userDAO = (UserDAO) servletContext.getAttribute(Initer.USER_DAO);
        }
        if (followingDAO == null) {
            followingDAO = (FollowingDAO) servletContext.getAttribute(Initer.FOLLOWING_DAO);
        }
        if (likeDAO == null)
            likeDAO = (LikeDAO) servletContext.getAttribute(Initer.LIKE_DAO);

        if (commentDAO == null)
            commentDAO = (CommentDAO) servletContext.getAttribute(Initer.COMMENT_DAO);
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Response getPostByUser(
            @QueryParam("userId") long userId,
            @QueryParam("visitorId") long visitorId,
            @QueryParam("offsetId") long offsetId,
            @QueryParam("limit") int limit)
            throws JsonProcessingException {

        log.info("getPostsByUser");
        log.info("visitorId: "+visitorId);

        ArrayList<Post> posts;

        if(visitorId == 0){

            posts = (ArrayList<Post>) postDAO.getNotPrivateUserPosts(userId, offsetId, limit);
        } else {

            posts = postDAO.getAllByUser(userId, offsetId, limit).stream()
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        log.info(Arrays.toString(posts.toArray()));
        Collection<PostView> postViews = createPostViews(posts);

        log.info(String.valueOf(postViews.size()));

        String json = JsonWrapper.toJson(postViews);

        return Response.ok(json).build();
    }

    @GET
    @Path("timeline/")
    @Produces(APPLICATION_JSON)
    public Response getTimeLine(
            @QueryParam("userId") long userId,
            @QueryParam("offsetId") long offsetId,
            @QueryParam("limit") int limit)
            throws JsonProcessingException {

        log.info("getUserTimeline");

        log.info("userId: "+userId);
        log.info("offsetId: "+offsetId);
        log.info("lmit: "+limit);

        Collection<Post> timeline =  postDAO.getUserTimeline(userId, offsetId, limit);

        Collection<PostView> pvTimeline = createPostViews(timeline);

        log.info(String.valueOf(pvTimeline.size()));

        String json = JsonWrapper.toJson(pvTimeline);

        return Response.ok(json).build();
    }

    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    public Response getPostById(@PathParam("id") long id) throws JsonProcessingException {

        Optional<Post> optPost = postDAO.getPostById(id);

        if(optPost.isPresent()){

            Post post = optPost.get();

            //noinspection OptionalGetWithoutIsPresent
            PostView postView = new PostView(
                    userDAO.getById(post.getAuthorId()).get(),
                    post,
                    likeDAO.countByPostId(post.getId()),
                    commentDAO.countByPostId(post.getId()));

            String json = JsonWrapper.toJson(postView);
            return Response.ok(json).build();
        } else {
            return Response.serverError().build();
        }
    }

    @POST
    @Path("delete/{postId}")
    public void deletePostById(@PathParam("postId") long postId){

        log.info("deleting post");
        postDAO.deletePost(postId);
    }

    @GET
    @Path("update")
    @Produces(APPLICATION_JSON)
    public Response updatePosts(@QueryParam("userId") long userId,
                                @QueryParam("visitorId") long visitorId,
                                @QueryParam("offsetId") long offsetId,
                                @QueryParam("limit") int limit)
                                throws JsonProcessingException, InterruptedException {
        log.info("updatePosts");
        log.info("offsetId: "+offsetId);

        while(!postDAO.isPostsReadyToUpdate(userId, offsetId))
            Thread.sleep(10000);

        ArrayList<Post> posts = postDAO.getAllByUser(userId, offsetId, limit).stream()
                .collect(Collectors.toCollection(ArrayList::new));

        if(visitorId == 0){

            posts = sortByPrivacy(posts);
        }

        Collection<PostView> postViews = createPostViews(posts);

        log.info(String.valueOf(postViews.size()));

        String json = JsonWrapper.toJson(postViews);

        return Response.ok(json).build();
    }

    @GET
    @Path("updatetimeline")
    @Produces(APPLICATION_JSON)
    public Response updateTimeline(
            @QueryParam("userId") long userId,
            @QueryParam("offsetId") long offsetId,
            @QueryParam("limit") int limit)
            throws JsonProcessingException, InterruptedException {

        log.info("update timeline");

        while (!postDAO.isTimelineReadyToUpdate(userId, offsetId))
            Thread.sleep(10000);

        Collection<Post> timeline =  postDAO.getUserTimeline(userId, offsetId, limit);

        Collection<PostView> pvTimeline = createPostViews(timeline);

        log.info(String.valueOf(pvTimeline.size()));

        String json = JsonWrapper.toJson(pvTimeline);

        return Response.ok(json).build();
    }

    @POST
    @Path("add")
    @Consumes(APPLICATION_JSON)
    public void addPost(final String params){

        HashMap<String, String> map = (HashMap<String, String>) parse(params);

        Post.PostBuilder postBuilder = Post.builder();

        Post newPost = postBuilder.authorId(Long.parseLong(map.get("userId")))
                .date(LocalDate.now())
                .time(LocalTime.now())
                .expandable(Boolean.parseBoolean(map.get("expandable")))
                .privacy(Boolean.parseBoolean(map.get("privacy")))
                .text(map.get("text"))
                .build();

        postDAO.addPost(newPost);
    }

    @GET
    @Path("getprev/")
    @Produces(APPLICATION_JSON)
    public Response getPrevByUser(
            @QueryParam("userId") long userId,
            @QueryParam("visitorId") long visitorId,
            @QueryParam("offsetId") long offsetId,
            @QueryParam("limit") int limit)
            throws JsonProcessingException {

        log.info("getPrevByUser");
        log.info("visitorId: "+visitorId);

        ArrayList<Post> posts;

        if(visitorId == 0){

            posts = (ArrayList<Post>) postDAO.getNotPrivatePrevPosts(userId, offsetId, limit);
        } else {

            posts = postDAO.getPrevByUser(userId, offsetId, limit).stream()
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        log.info(Arrays.toString(posts.toArray()));
        Collection<PostView> postViews = createPostViews(posts)
                .stream()
                .sorted((p1, p2) -> Long.compare(p2.getPost().getId(), p1.getPost().getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        log.info(String.valueOf(postViews.size()));

        String json = JsonWrapper.toJson(postViews);

        return Response.ok(json).build();
    }

    @GET
    @Path("getprevtimeline/")
    @Produces(APPLICATION_JSON)
    public Response getPrevTimeline(
            @QueryParam("userId") long userId,
            @QueryParam("offsetId") long offsetId,
            @QueryParam("limit") int limit)
            throws JsonProcessingException {
        ArrayList<Post> posts;

        posts = postDAO.getPrevTimeline(userId, offsetId, limit).stream()
                .collect(Collectors.toCollection(ArrayList::new));

        log.info(Arrays.toString(posts.toArray()));

        Collection<PostView> postViews = createPostViews(posts)
                .stream()
                .sorted((p1, p2) -> Long.compare(p2.getPost().getId(), p1.getPost().getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        log.info(String.valueOf(postViews.size()));

        String json = JsonWrapper.toJson(postViews);

        return Response.ok(json).build();
    }

    @GET
    @Path("getTimelineCount/{userId}")
    @Produces(APPLICATION_JSON)
    public Response getTimelineCount(@PathParam("userId") long userId)
            throws JsonProcessingException {

        int timelineCount = postDAO.countPostsInTimeline(userId);

        String json = JsonWrapper.toJson(timelineCount);

        return Response.ok(json).build();
    }

    @SuppressWarnings("Duplicates")
    private Collection<PostView> createPostViews(Collection<Post> posts){

        ArrayList<PostView> postViews = new ArrayList<>();

        PostView.PostViewBuilder postViewBuilder = PostView.builder();

        //noinspection OptionalGetWithoutIsPresent
        posts.forEach(p -> postViews.add(
                postViewBuilder
                        .user(userDAO.getById(p.getAuthorId()).get())
                        .post(p)
                        .likesCount(likeDAO.countByPostId(p.getId()))
                        .commentsCount(commentDAO.countByPostId(p.getId()))
                        .build()));

//        postViews.stream().sorted(Comparator.comparing(pV -> pV.getPost().getId()));
        postViews.sort(Comparator.comparing(pv -> pv.getPost().getId()));
        postViews.forEach(pv -> log.info("id: {}", pv.getPost().getId()));
        return postViews;
    }

    private Map<String,String> parse(String params){

        String[] param = params.split("&");

        return Stream.of(param).collect(Collectors.toMap(
                p -> p.split("=")[0],
                p -> p.split("=")[1]
        ));
    }

    private ArrayList<Post> sortByPrivacy(Collection<Post> posts){

        log.info("sorting by privacy");
        return posts.stream()
                .filter(p -> !p.isPrivacy())
                .collect(Collectors.toCollection(ArrayList::new));
    }

//    @POST
//    @Path("create")
//    public Response createPost(
//            @QueryParam("userId") long userId,
//            @QueryParam("text") String text,
//            @QueryParam("expandable") boolean expandable,
//            @QueryParam("privacy") boolean privacy){
//
//        Post.PostBuilder postBuilder = Post.builder();
//
//        postDAO.addPost(
//                postBuilder
//                        .authorId(userId)
//                        .date(LocalDate.now())
//                        .time(LocalTime.now())
//                        .text(text)
//                        .expandable(expandable)
//                        .privacy(privacy)
//                        .build());
//    }
}
