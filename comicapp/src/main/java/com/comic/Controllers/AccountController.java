package com.comic.Controllers;

import com.comic.Service.*;
import com.comic.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.List;

@Controller
public class AccountController {

    @Autowired
    S3Services s3Services;

    @Autowired
    private UserService userService;

    @Autowired
    private SeriesService seriesService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private ComicService comicService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private DislikeService dislikeService;

    @RequestMapping(value = {"/account"}, method = RequestMethod.GET)
    public ModelAndView account() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("profilepage");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByEmail(auth.getName());
        System.out.println("User: " + user);
        List<Series> series = seriesService.findAllSeries();
        List<Series> seriesList = new ArrayList<>();
        for (Series s : series) {
            if (s.getAuthorUsername().equals(user.getUsername())) {
                seriesList.add(s);
            }
        }
        List<Subscription> subscribers = subscriptionService.findBySubscriberUsername(user.getUsername());
        System.out.println(subscribers);
        List<User> subscriptions = new ArrayList<>();
        for (Subscription s : subscribers) {
            User u = userService.findUserByUsername(s.getSubscribeeUsername());
            subscriptions.add(u);
        }
        List<Comic> orderedComics = comicService.findLatestComics();
        List<Comic> latestComics = new ArrayList<>();
        int i = 0;
        for (Comic o: orderedComics) {
            Series s = seriesService.findSeriesById(o.getSeriesId());
            System.out.println("Series:" + series);
            System.out.println("service:" + subscriptionService);
            if (subscriptionService.findIfSubscriptionExists(s.getAuthorUsername(), user.getUsername()) != null) {
                latestComics.add(o);
                i++;
                if (i == 10) break;
            }
        }
        User currentUser = userService.findUserByEmail(auth.getName());
        modelAndView.addObject("user", user);
        modelAndView.addObject("series", seriesList);
        System.out.println("Subscriptions: " + subscriptions);
        modelAndView.addObject("subscriptions", subscriptions);
        modelAndView.addObject("latestComics", latestComics);
        modelAndView.addObject("currentUser", currentUser);
        return modelAndView;
    }

    @RequestMapping(value = {"/account/subscribe/{username}"}, method = RequestMethod.GET)
    public ModelAndView subscribe(@PathVariable("username") String username) {
        Subscription newSubscription = new Subscription();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findUserByEmail(auth.getName());
        newSubscription.setSubscriberUsername(currentUser.getUsername());
        User author = userService.findUserByUsername(username);
        newSubscription.setSubscribeeUsername(author.getUsername());
        subscriptionService.saveSubscription(newSubscription);
        ModelAndView modelAndView = new ModelAndView(new RedirectView("/account"));
        return modelAndView;
    }

    @RequestMapping(value = {"/account/unsubscribe/{username}"}, method = RequestMethod.GET)
    public ModelAndView unsubscribe( @PathVariable("username") String username){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User author = userService.findUserByUsername(username);
        User currentUser = userService.findUserByEmail(auth.getName());
        Subscription subscription = subscriptionService.findIfSubscriptionExists(author.getUsername(),currentUser.getUsername());
        subscriptionService.deleteSubscription(subscription);
        ModelAndView modelAndView = new ModelAndView(new RedirectView("/account"));
        return modelAndView;

    }

    @RequestMapping(value = {"/account/series/{id:[\\d]+}"},method = RequestMethod.GET)
    public ModelAndView manageComics(@PathVariable("id") int id){
        ModelAndView modelAndView = new ModelAndView();
        Series series = seriesService.findSeriesById(id);
        if(series == null){
            modelAndView = new ModelAndView(new RedirectView("/account" ));
            return modelAndView;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByEmail(auth.getName());
        System.out.println(!series.getAuthorUsername().equalsIgnoreCase(user.getUsername()));
        if(!series.getAuthorUsername().equalsIgnoreCase(user.getUsername())){
            modelAndView = new ModelAndView(new RedirectView("/account" ));
            return modelAndView;
        }
        List<Comic> comics = comicService.findComicsBySeriesId(series.getId());
        System.out.println(series);
        System.out.println(comics);
        System.out.println(user);
        modelAndView.addObject("currentUser",user);
        modelAndView.addObject("series",series);
        modelAndView.addObject("comics",comics);
        modelAndView.setViewName("manageComics");
        return  modelAndView;

    }

    @RequestMapping(value = {"account/series/comic/delete"}, method = RequestMethod.POST)
    public ModelAndView deleteComic(@ModelAttribute Comic comic) {
        ModelAndView modelAndView;
        comic = comicService.findComicById(comic.getId());
        int seriesId = comic.getSeriesId();
        int comicId = comic.getId();
        comicService.deleteComic(comic);
        List<Comment> comments = commentService.findCommentsByComicId(comicId);
        for (Comment comment : comments) {
            commentService.deleteComment(comment);
        }
        List<Like> likes = likeService.findByComicId(comicId);
        for (Like like : likes) {
            likeService.deleteLike(like);
        }
        List<Dislike> dislikes = dislikeService.findByComicId(comicId);
        for (Dislike dislike : dislikes) {
            dislikeService.deleteDislike(dislike);
        }
        s3Services.deleteFileFromS3Bucket("series" +seriesId+"comic"+comicId+ ".png" );
        s3Services.deleteFileFromS3Bucket("series" + seriesId + "comic" + comicId + ".json");
        modelAndView = new ModelAndView(new RedirectView("/account/series/" + seriesId));
        return modelAndView;
    }

    @RequestMapping(value = {"/account/series/makepublic"}, method = RequestMethod.POST)
    public ModelAndView makePublic(@ModelAttribute Comic comic){
        ModelAndView modelAndView;
        comic = comicService.findComicById(comic.getId());
        comic.setPublicComic(true);
        comicService.saveComic(comic);
        Series series =seriesService.findSeriesById(comic.getSeriesId());
        modelAndView = new ModelAndView(new RedirectView("/account/series/" + series.getId()));
        return modelAndView;
    }

    @RequestMapping(value = {"/account/series/makeprivate"}, method = RequestMethod.POST)
    public ModelAndView makePrivate(@ModelAttribute Comic comic){
        ModelAndView modelAndView;
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        User currentUser = userService.findUserByEmail(auth.getName());
        comic = comicService.findComicById(comic.getId());
        comic.setPublicComic(false);
        comicService.saveComic(comic);
        Series series =seriesService.findSeriesById(comic.getSeriesId());
        modelAndView = new ModelAndView(new RedirectView("/account/series/" + series.getId()));
        return modelAndView;
    }


    @RequestMapping(value = {"/profileSettings"},method = RequestMethod.GET)
    public ModelAndView profileSettings() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("profileSettings");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findUserByEmail(auth.getName());
        modelAndView.addObject("currentUser", currentUser);
        return modelAndView;
    }
}
