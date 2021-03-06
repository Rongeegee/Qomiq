package com.comic.Controllers;

import com.amazonaws.services.s3.model.S3Object;
import com.comic.Bean.GameForm;
import com.comic.Service.*;
import com.comic.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Controller
public class TelephoneController {

    @Autowired
    public UserService userService;

    @Autowired
    public GameService gameService;

    @Autowired
    public GamePageService gamePageService;

    @Autowired
    public SubscriptionService subscriptionService;

    @Autowired
    public GamePlayerService gamePlayerService;

    @Autowired
    public ComicService comicService;

    @Autowired
    public SeriesService  seriesService;

    @Autowired
    public SubmissionService submissionService;

    @Autowired
    S3Services s3Services;

    @Autowired
    VoteService voteService;

    @RequestMapping(value = {"/play"}, method = RequestMethod.GET)
    public ModelAndView play() {
        ModelAndView modelAndView = new ModelAndView();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByEmail(auth.getName());
        List<Series> series;
        if (user != null)
            series = seriesService.findAllSeriesByAuthorUsername(user.username);
        else{
            series = null;
        }
        List<Comic> comicList = new ArrayList<>();
        for(Series s: series){
            comicList.addAll(comicService.findComicsBySeriesId(s.getId()));
        }
        List<Game> games = gameService.findAllGames();
        List<Game> myGames = new ArrayList<>();
        for(Game game: games){
            if(gamePlayerService.findGamePlayerByUserIdAndGameId(user.getId(),game.getId()) != null){
                myGames.add(game);
            }
        }
        modelAndView.addObject("gameForm", new GameForm());
        modelAndView.addObject("comics", comicList);
        modelAndView.addObject("games", myGames);
        modelAndView.addObject("newGame",new Game());
        modelAndView.addObject("currentUser", user);
        modelAndView.setViewName("telephonegame");
        return modelAndView;
    }

    @RequestMapping(value = {"/game/new"}, method = RequestMethod.POST)
    public ModelAndView newGame(@ModelAttribute("gameForm") GameForm gameForm) {
        ModelAndView modelAndView = new ModelAndView();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByEmail(auth.getName());
        Comic comic = comicService.findComicById(gameForm.getComicName());
        Series series = seriesService.findSeriesById(comic.getSeriesId());
        String keyName1 = "comicCover"+comic.getId()+".json";
        String keyName2 = "comicCover"+comic.getId()+".png";
        S3Object output1 = s3Services.downloadFile(keyName1);
        S3Object output2 = s3Services.downloadFile(keyName2);
        Game game = new Game();
        game.setGameName(gameForm.getGameName());
        game.setUserId(user.getId());
        game.setComicId(comic.getId());
        game = gameService.saveGame(game);
        List<Subscription> subscriptions = subscriptionService.findBySubscribeeUsername(user.getUsername());
        for(Subscription s : subscriptions){
            GamePlayer gamePlayer = new GamePlayer();
            User subscriber = userService.findUserByUsername(s.getSubscriberUsername());
            gamePlayer.setGameId(game.getId());
            gamePlayer.setUserId(subscriber.getId());
            gamePlayerService.savePlayer(gamePlayer);
        }
        GamePlayer gamePlayer = new GamePlayer();
        gamePlayer.setGameId(game.getId());
        gamePlayer.setUserId(user.getId());
        gamePlayerService.savePlayer(gamePlayer);
        GamePage gamePage = new GamePage();
        gamePage.setGameId(game.getId());
        gamePage.setPageNumber(1);
        gamePage.setFinished(false);
        gamePage = gamePageService.saveGamePage(gamePage);
        game.setCurrentPage(gamePage.getId());
        gameService.saveGame(game);
        keyName1 = "gamePage"+gamePage.getId()+".json";
        keyName2 = "gamePage"+gamePage.getId()+".png";
        s3Services.uploadFile(keyName1,output1);
        s3Services.uploadFile(keyName2,output2);
        modelAndView = new ModelAndView(new RedirectView("/game/"+game.getId()));
        return modelAndView;
    }

    @RequestMapping(value = "game/{id:[\\d]+}" , method = RequestMethod.GET)
    public ModelAndView game(@PathVariable("id") int id){
        ModelAndView modelAndView = new ModelAndView();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByEmail(auth.getName());
        Game game = gameService.findGameById(id);
        List<GamePage> gamePages = gamePageService.findGamePagesByGameId(id);
        List<GamePlayer> players = gamePlayerService.findGamePlayersByGameId(game.getId());
        List<User> users = new LinkedList<>();
        for(GamePlayer player: players){
            users.add(userService.findUserById(player.getUserId()));
        }
        modelAndView.addObject("gamePages", gamePages);
        modelAndView.addObject("users", users);
        modelAndView.addObject("players",players );
        modelAndView.addObject("currentUser", user);
        modelAndView.addObject("game", game);
        modelAndView.setViewName("gameMenu");
        return modelAndView;
    }

    @RequestMapping(value = {"/gameEdit/{id:[\\d]+}"}, method = RequestMethod.GET)
    public ModelAndView edit(@PathVariable("id") int id){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("fileName", "gamePage"+id+".json");
        modelAndView.addObject("gamePageId", id);
        modelAndView.setViewName("gameeditor");
        return modelAndView;
    }

    @RequestMapping(value = {"/gameSubmissions/{id:[\\d]+}"}, method = RequestMethod.GET)
    public ModelAndView gameSubmissions(@PathVariable("id") int id){
        ModelAndView modelAndView = new ModelAndView();
        GamePage gamePage = gamePageService.findGamePageById(id);
        List<Submission> submissions = submissionService.findAllSubmissionsByGamePageId(gamePage.getId());
        modelAndView.addObject("submissions", submissions);
        modelAndView.setViewName("gameSubmissions");
        return  modelAndView;
    }

    @RequestMapping(value = {"/endRound/{id:[\\d]+}"}, method = RequestMethod.GET)
    public ModelAndView endRound(@PathVariable("id") int  id){
        Game game = gameService.findGameById(id);
        GamePage gamePage = gamePageService.findGamePageById(game.getCurrentPage());
        List<Submission> submissions = submissionService.findAllSubmissionsByGamePageId(gamePage.getId());
        Submission popularSubmission = submissions.get(0);
        for(Submission submission : submissions){
            if(submission.getVotes() > popularSubmission.getVotes())
                popularSubmission = submission;
        }
        String keyName1 = "submission"+popularSubmission.getId()+".json";
        String keyName2 = "submission"+popularSubmission.getId()+".png";
        S3Object output1 = s3Services.downloadFile(keyName1);
        S3Object output2 = s3Services.downloadFile(keyName2);
        S3Object output3 = s3Services.downloadFile(keyName1);
        S3Object output4 = s3Services.downloadFile(keyName2);
        keyName1 = "pageWinner"+gamePage.getId()+".json";
        keyName2 = "pageWinner"+gamePage.getId()+".png";
        s3Services.uploadFile(keyName1,output1);
        s3Services.uploadFile(keyName2,output2);
        //pageWinner(id).png
        gamePage.setPageWinner(gamePage.getId());
        gamePage.setPageWinnerVotes(popularSubmission.getVotes());
        gamePage.setFinished(true);
        gamePage.setTitle(popularSubmission.getTitle());
        gamePageService.saveGamePage(gamePage);
        GamePage gamePage2 = new GamePage();
        gamePage2.setPageNumber(gamePage.getPageNumber()+1);
        gamePage2.setGameId(game.getId());
        gamePage2.setFinished(false);
        gamePage2 = gamePageService.saveGamePage(gamePage2);
        keyName1 = "gamePage"+gamePage2.getId()+".json";
        keyName2 = "gamePage"+gamePage2.getId()+".png";
        s3Services.uploadFile(keyName1,output3);
        s3Services.uploadFile(keyName2,output4);
        game.setCurrentPage(gamePage2.getId());
        gameService.saveGame(game);
        ModelAndView modelAndView = new ModelAndView(new RedirectView("/game/"+game.getId()));
        return modelAndView;
    }

    @RequestMapping(value = {"/endGame/{id:[\\d]+}"}, method = RequestMethod.GET)
    public ModelAndView endGame(@PathVariable("id") int  id){
        Game game = gameService.findGameById(id);
        List<GamePage> gamePages =  gamePageService.findGamePagesByGameId(game.getId());
        List<GamePage> finishedPages = new ArrayList<>();
        for(GamePage gamePage : gamePages){
            if(gamePage.isFinished() == true){
                finishedPages.add(gamePage);
            }
        }
        GamePage gameWinner = finishedPages.get(0);
        for(GamePage gamePage : finishedPages){
            if(gamePage.getPageWinnerVotes() > gameWinner.getPageWinnerVotes()){
                gameWinner = gamePage;
            }
        }
        String keyName = "pageWinner" + gameWinner.getId() + ".png";
        S3Object output = s3Services.downloadFile(keyName);
        keyName = "gameWinner" + game.getId() + ".png";
        s3Services.uploadFile(keyName,output);
        game.setFinished(true);
        game.setWinner(game.getId());
        game.setGameName(gameWinner.getTitle());
        gameService.saveGame(game);
        ModelAndView modelAndView = new ModelAndView(new RedirectView("/game/" + game.getId()));
        return  modelAndView;
    }

    @RequestMapping(value = {"game/vote/{id:[\\d]+}"}, method = RequestMethod.GET)
    public ModelAndView vote(@PathVariable("id") int id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findUserByEmail(auth.getName());
        Submission submission = submissionService.findSubmissionById(id);
        GamePage gamePage = gamePageService.findGamePageById(submission.getGamePageId());
        Vote vote = voteService.findVoteByGamePageIdAndVoterUsername(gamePage.getId(),currentUser.getUsername());
        int gamePageId;
        if(vote == null){
            vote = new Vote();
            vote.setVoterUsername(currentUser.getUsername());
            vote.setSubmissionId(submission.getId());
            vote.setGamePageId(submission.getGamePageId());
            gamePageId = submission.getGamePageId();
            voteService.saveVote(vote);
            submission.setVotes(submission.getVotes()+1);
            submissionService.saveSubmission(submission);
        }
        else{
            if(submission.getId() == vote.getSubmissionId()){
                submission.setVotes(submission.getVotes() - 1);
                submissionService.saveSubmission(submission);
                gamePageId = submission.getGamePageId();
                voteService.deleteVote(vote);
            }
            else{
                submissionService.findSubmissionById(vote.getSubmissionId());
                submission.setVotes(submission.getVotes() - 1);
                voteService.deleteVote(vote);
                vote = new Vote();
                vote.setVoterUsername(currentUser.getUsername());
                vote.setSubmissionId(submission.getId());
                vote.setGamePageId(gamePage.getId());
                gamePageId = gamePage.getId();
                voteService.saveVote(vote);
                submission.setVotes(submission.getVotes()+1);
                submissionService.saveSubmission(submission);

            }
        }
        ModelAndView modelAndView = new ModelAndView(new RedirectView("/gameSubmissions/"+gamePageId));
        return  modelAndView;
    }

}
