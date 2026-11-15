package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.List;

/** Freely licensed Blender films hosted by the filmmakers. Metadata and source credits retained in UI. */
public final class OpenFilms {
    public static final class Film {
        public final String id,title,year,genre,summary,url,license;
        Film(String id,String title,String year,String genre,String summary,String url,String license){
            this.id=id;this.title=title;this.year=year;this.genre=genre;
            this.summary=summary;this.url=url;this.license=license;
        }
    }
    public static List<Film> all(){
        List<Film> items=new ArrayList<>();
        items.add(new Film("bunny","Big Buck Bunny","2008","Animation · Comedy","A gentle giant bunny faces three bullying rodents.",
          "https://video.blender.org/object-storage/web_videos/bf1f3fb5-b119-4f9f-9930-8e20e892b898-720.mp4","© Blender Foundation — CC BY 3.0"));
        items.add(new Film("sintel","Sintel","2010","Animation · Fantasy","A girl travels through a harsh land in search of her lost dragon.",
          "https://video.blender.org/object-storage/web_videos/0eb052d0-fd51-43e6-aa33-ecdbf77a5d40-720.mp4","© Blender Foundation — CC BY 3.0"));
        items.add(new Film("tears","Tears of Steel","2012","Sci-Fi · Live Action","An open sci-fi film set in a robot-threatened Amsterdam.",
          "https://video.blender.org/object-storage/web_videos/8533ea43-4271-4a57-9694-e9d0b35e1aa1-720.mp4","© Blender Institute — CC BY 3.0"));
        items.add(new Film("spring","Spring","2019","Animation · Fantasy","A shepherd girl and her dog meet ancient spirits.",
          "https://video.blender.org/object-storage/web_videos/3d95fb3d-c866-42c8-9db1-fe82f48ccb95-720.mp4","© Blender Studio — CC BY 4.0"));
        items.add(new Film("sprite","Sprite Fright","2021","Animation · Horror Comedy","A forest adventure takes an unexpected turn.",
          "https://video.blender.org/object-storage/web_videos/a69d68a5-a0e0-4a80-9d66-49f093c97aaf-720.mp4","© Blender Studio — CC BY 4.0"));
        items.add(new Film("agent","Agent 327: Operation Barbershop","2017","Animation · Action","An adventurous short film featuring Agent 327.",
          "https://video.blender.org/object-storage/web_videos/264ff760-803e-430e-8d81-15648e904183-720.mp4","© Blender Studio — CC BY 4.0"));
        return items;
    }
    private OpenFilms(){}
}
