package com.comic.Repository;

import com.comic.model.Series;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeriesRepository extends JpaRepository<Series, Integer> {
        public List<Series> findAll();

        public List<Series> findAllByOrderByLastModDateDesc();

        public List<Series> findAllByOrderBySeriesViewsDesc();

        public List<Series> findTop5ByOrderBySeriesViewsDesc();

        public List<Series> findAllByAuthorUsername(String authorUserName);

        public Series findSeriesById(int id);
}
