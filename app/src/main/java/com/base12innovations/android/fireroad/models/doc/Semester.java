package com.base12innovations.android.fireroad.models.doc;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Semester {

    public interface Delegate{
        int getNumYears();
        Map<Semester,String> getSemesterNames();
        Map<Semester,String> getSemesterIDs();
        void updateNumYears(int newNumYears);
    }

    private WeakReference<Delegate> delegate;

    public enum Season{
        Fall(0),IAP(1),Spring(2),Summer(3),Undefined(9);
        private int rawvalue;
        Season(int rawvalue){
            this.rawvalue = rawvalue;
        }
        public Season next(){
            return values()[(rawvalue+1)%4];
        }
        public Season prev(){
            return values()[(rawvalue+3)%4];
        }
    }

    private boolean isValid;
    private boolean isPriorCredit;
    private int year;
    private Season season;

    public boolean isValid() {
        return isValid;
    }
    public boolean isPriorCredit() {
        return isPriorCredit;
    }
    public Season getSeason() {
        return season;
    }
    public int getYear() {
        return year;
    }

    public static class SemesterID{
        public static final String fall = "fall-";
        public static final String spring = "spring-";
        public static final String iap = "iap-";
        public static final String summer = "summer-";
        public static final String priorCredit = "prior-credit";
    }

    public Semester(){
        this(false);
    }

    public Semester(boolean isPriorCredit){
        init(isPriorCredit,isPriorCredit);
    }

    public Semester(String semesterID, boolean updateNumYears, WeakReference<Delegate> delegate){
        if(semesterID.equals(SemesterID.priorCredit)){
            init(true,true, delegate);
        }else {
            int year = -1;
            Season season = Season.Fall;
            if (semesterID.startsWith(SemesterID.fall)) {
                year = Integer.valueOf(semesterID.substring(SemesterID.fall.length()));
                season = Season.Fall;
            } else if (semesterID.startsWith(SemesterID.iap)) {
                year = Integer.valueOf(semesterID.substring(SemesterID.iap.length()));
                season = Season.IAP;
            } else if (semesterID.startsWith(SemesterID.spring)) {
                year = Integer.valueOf(semesterID.substring(SemesterID.spring.length()));
                season = Season.Spring;
            } else if (semesterID.startsWith(SemesterID.summer)) {
                year = Integer.valueOf(semesterID.substring(SemesterID.summer.length()));
                season = Season.Summer;
            }
            init(year,season,updateNumYears,delegate);
        }
    }

    public Semester(int oldSemesterIndex, boolean updateNumYears, WeakReference<Delegate> delegate){
        if(oldSemesterIndex == 0){
            init(true,true,delegate);
        }else if(oldSemesterIndex > 0){
            int year = (oldSemesterIndex+2)/3;
            int season = (oldSemesterIndex+2)%3;
            init(year,Season.values()[season],updateNumYears,delegate);
        }else{
            init(false,false,delegate);
        }
    }

    public Semester(int year, Season season, boolean overrideYearCheck){
        if(overrideYearCheck){
            this.isValid = true;
            this.isPriorCredit = false;
            this.year = year;
            this.season = season;
            this.delegate = null;
        }else{
            init(year,season);
        }
    }
    public Semester(int year, Season season){
        init(year,season);
    }

    private void init(int year, Season season) {
        init(year,season,false);
    }
    private void init(int year, Season season, boolean updateNumYears){
        init(year,season,updateNumYears,new WeakReference<Delegate>(User.currentUser().getCurrentDocument()));
    }
    private void init(int year, Season season, boolean updateNumYears, WeakReference<Delegate> delegate){
        this.delegate = delegate;
        if(updateNumYears)
            delegate.get().updateNumYears(year);
        isValid = year > 0 && year <= delegate.get().getNumYears();
        if(isValid) {
            this.year = year;
            this.season = season;
        }else{
            this.year = -1;
            this.season = Season.Undefined;
        }
        isPriorCredit = false;
    }
    private void init(boolean isValid, boolean isPriorCredit){
        init(isValid,isPriorCredit,new WeakReference<Delegate>(User.currentUser().getCurrentDocument()));
    }
    private void init(boolean isValid, boolean isPriorCredit, WeakReference<Delegate> delegate){
        this.delegate = delegate;
        this.isValid = isValid;
        this.isPriorCredit = isPriorCredit;
        this.year = -1;
        this.season = Season.Undefined;
    }

    int oldSemesterIndex(){
        if(isValid){
            if(isPriorCredit)
                return 0;
            if(season == Season.Summer)
                return 0;
            if(year > 5)
                return 0;
            return year*3+season.rawvalue-2;
        }
        return 0;
    }

    public Semester prevSemester(){
        if(!isValid || isPriorCredit)
            return new Semester();
        if(year == 1 && season == Season.Fall)
            return new Semester(true);
        Season newSeason = season.prev();
        int newYear = year;
        if(newSeason == Season.Summer)
            newYear--;
        return new Semester(newYear,newSeason);
    }

    public Semester nextSemester(){
        if(!isValid)
            return new Semester();
        if(isPriorCredit)
            return new Semester(1,Season.Fall);
        Season newSeason = season.next();
        int newYear = year;
        if(newSeason == Season.Fall)
            newYear++;
        return new Semester(newYear,newSeason);
    }

    public String semesterID(){
        if(delegate != null && delegate.get().getSemesterIDs().containsKey(this))
            return delegate.get().getSemesterIDs().get(this);
        if(isValid){
            if(isPriorCredit)
                return SemesterID.priorCredit;
            switch(season){
                case Fall:
                    return SemesterID.fall + year;
                case IAP:
                    return SemesterID.iap + year;
                case Spring:
                    return SemesterID.spring + year;
                case Summer:
                    return SemesterID.summer + year;
            }
        }
        return "Invalid SemesterID";
    }

    public boolean isBefore(Semester otherSemester){
        if(isPriorCredit)
            return otherSemester.isValid && !otherSemester.isPriorCredit;
        if(year < otherSemester.year){
            return true;
        }else return year == otherSemester.year && season.rawvalue < otherSemester.season.rawvalue;
    }
    public boolean isBeforeOrEqual(Semester otherSemester){
        if(isPriorCredit)
            return true;
        if(year < otherSemester.year){
            return true;
        }else return year == otherSemester.year && season.rawvalue <= otherSemester.season.rawvalue;
    }

    public static String yearString(int year){
        int ones = year % 10;
        int tens = year / 10;
        String modifier = "th";
        if(tens != 1){
            switch(ones){
                case 1:{
                    modifier = "st";
                    break;
                }
                case 2:{
                    modifier = "nd";
                    break;
                }
                case 3:{
                    modifier = "rd";
                    break;
                }
            }
        }
        return year + modifier + " Year";
    }

    @Override
    public String toString(){
        if(delegate != null && delegate.get().getSemesterNames().containsKey(this))
            return delegate.get().getSemesterNames().get(this);
        if(!isValid)
            return "Invalid Semester Value";
        if(isPriorCredit)
            return "Prior Credit";
        return yearString(year) + " " + season;
    }

    @Override
    public int hashCode(){
        return Objects.hash(isValid,isPriorCredit,year,season);
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof  Semester)){
            return false;
        }
        Semester semesterObj = (Semester) obj;
        return semesterObj.isPriorCredit == this.isPriorCredit
                && semesterObj.isValid == this.isValid
                && semesterObj.year == this.year
                && semesterObj.season == this.season;
    }

}

