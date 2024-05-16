package com.example.akillidurak;

public class BusStop {
    private long id;
    private String type;
    private double lat;
    private double lon;
    private Tags tags;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    public static class Tags {
        private String name;
        private String highway;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHighway() {
            return highway;
        }

        public void setHighway(String highway) {
            this.highway = highway;
        }
    }
}
