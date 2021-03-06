package com.comic.Controllers;
import com.amazonaws.services.s3.model.S3Object;
import com.comic.Forms.ExploreForm;
import com.comic.Service.ComicService;
import com.comic.Service.S3Services;
import com.comic.Service.SeriesService;
import com.comic.Service.UserService;
import com.comic.model.Comic;
import com.comic.model.Pages;
import com.comic.model.Series;
import com.comic.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.jws.WebParam;
import java.io.File;
import java.io.InputStream;
import java.util.List;

@Controller
public class CreateController {

    @Autowired
    UserService userService;

    @Autowired
    SeriesService seriesService;

    @Autowired
    ComicService comicService;

    @Autowired
    S3Services s3Services;

    @RequestMapping(value = {"/create"}, method = RequestMethod.GET)
    public ModelAndView create() {
        ModelAndView modelAndView = new ModelAndView();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByEmail(auth.getName());
        List<Series> series;
        if (user != null)
            series = seriesService.findAllSeriesByAuthorUsername(user.username);
        else{
            series = null;
        }
        modelAndView.setViewName("editor");
        modelAndView.addObject("currentUser", user);
        modelAndView.addObject("series", series);
        return modelAndView;
    }

    @RequestMapping(value = {"/create/editpage"}, method = RequestMethod.POST)
    public ModelAndView editpage(@ModelAttribute Pages page){
        ModelAndView modelAndView = new ModelAndView();
        Comic comic = comicService.findComicById(page.getComicId());
        modelAndView.addObject("comic",comic);
        modelAndView.addObject("page", page);
        modelAndView.setViewName("editEditor");
        return modelAndView;
    }

    @RequestMapping(value={"/create/editpage/"}, method = RequestMethod.GET)
    public ModelAndView editpage(@RequestParam("id") int id) {
        ModelAndView modelAndView = new ModelAndView(new RedirectView("/account/"));
        return modelAndView;
    }

    @RequestMapping(value={"/create/newpage"}, method = RequestMethod.POST)
    public ModelAndView newpage(@ModelAttribute Comic comic) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("comic", comic);
        modelAndView.setViewName("newPageEditor");
        return modelAndView;
    }

    @RequestMapping(value={"/create/newpage/"}, method = RequestMethod.GET)
    public ModelAndView newpage(@RequestParam("id") int id) {
        ModelAndView modelAndView = new ModelAndView(new RedirectView("/account/"));
        return modelAndView;
    }
}
