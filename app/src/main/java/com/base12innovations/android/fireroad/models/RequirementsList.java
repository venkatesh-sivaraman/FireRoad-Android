package com.base12innovations.android.fireroad.models;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequirementsList extends RequirementsListStatement {

    public String shortTitle, mediumTitle, titleNoDegree, listID;
    public File fileLocation;
    public URL webURL;
    public boolean isLoaded = false;

    public RequirementsList(File location) {
        String path = location.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
        this.listID = fileName;
        this.fileLocation = location;
        String contents = readRequirementsFile(location);
        if (contents.length() > 0)
            parseRequirementsList(contents, true);
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public String readRequirementsFile(File fl) {
        try {
            FileInputStream fin = new FileInputStream(fl);

            try {
                String ret = convertStreamToString(fin);
                fin.close();
                return ret;
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void loadIfNeeded() {
        if (!isLoaded && fileLocation != null) {
            String contents = readRequirementsFile(fileLocation);
            if (contents.length() > 0)
                parseRequirementsList(contents, false);
            isLoaded = true;
        }
    }

    @Override
    public String toString() {
        loadIfNeeded();
        return super.toString();
    }

    @Override
    public List<RequirementsListStatement> getRequirements() {
        loadIfNeeded();
        return super.getRequirements();
    }

    private class TopLevelSection {
        String title;
        String description;

        TopLevelSection(String t, String d) {
            title = t;
            description = d;
        }
    }

    void parseRequirementsList(String string, boolean partial) {
        // Get a list of meaningful lines
        String[] linesTemp = string.split("\\n");
        List<String> lines = new ArrayList<>();
        for (String line : linesTemp) {
            if (line.contains(commentCharacter)) {
                if (line.indexOf(commentCharacter) == 0)
                    continue;
                else
                    lines.add(line.substring(0, line.indexOf(commentCharacter)).trim());
            } else {
                lines.add(line.trim());
            }
        }

        // Parse the first two lines
        String headerLine = lines.remove(0);
        List<String> headerComps = undecoratedSplit(headerLine, headerSeparator);
        if (headerComps.size() > 0) {
            shortTitle = headerComps.remove(0);
            if (headerComps.size() > 0) {
                mediumTitle = headerComps.remove(0);
                if (headerComps.size() > 1) {
                    titleNoDegree = headerComps.remove(0);
                    title = headerComps.remove(0);
                } else if (headerComps.size() > 0) {
                    title = headerComps.remove(0);
                }

                for (String comp : headerComps) {
                    String noWhitespaceComp = comp.replaceAll("\\s+", "");
                    if (noWhitespaceComp.contains(thresholdParameter)) {
                        int thresholdValue = Integer.parseInt(noWhitespaceComp.substring(noWhitespaceComp.indexOf(thresholdParameter) + thresholdParameter.length()));
                        threshold = new Threshold(ThresholdType.GREATER_THAN_OR_EQUAL, thresholdValue, ThresholdCriterion.SUBJECTS);
                    } else if (noWhitespaceComp.contains(urlParameter)) {
                        try {
                            webURL = new URL(noWhitespaceComp.substring(noWhitespaceComp.indexOf(urlParameter) + urlParameter.length()));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (partial) {
            return;
        }

        // Second line is the description of the course
        String descriptionLine = lines.remove(0);
        Log.d("RequirementsList", "Content description " + descriptionLine);
        if (descriptionLine.length() > 0) {
            contentDescription = descriptionLine.replaceAll("\\\\n", "\n");
        }
        Log.d("RequirementsList", "Now content description " + descriptionLine);

        if (lines.size() == 0) {
            Log.e("RequirementsList", listID + ": Reached end of file early!");
            return;
        }
        if (lines.get(0).length() > 0) {
            Log.e("RequirementsList", listID + ": Third line isn't empty, contains " + lines.get(0));
            return;
        }
        lines.remove(0);

        List<TopLevelSection> topLevelSections = new ArrayList<>();
        while (lines.size() > 0 && lines.get(0).length() > 0) {
            if (lines.size() <= 2) {
                Log.e("RequirementsList", listID + ": Not enough lines for top-level sections - need variable names and descriptions on two separate lines.");
                return;
            }
            String varName = undecoratedComponent(lines.remove(0));
            String description = undecoratedComponent(lines.remove(0).replaceAll("\\\\n", "\n"));
            if (varName.contains(":=") || description.contains(":="))
                Log.e("RequirementsList", listID + ": Encountered ':=' symbol in top-level section. Maybe you forgot the required empty line after the last section's description line?");
            topLevelSections.add(new TopLevelSection(varName, description));
        }

        if (lines.size() == 0) {
            Log.e("RequirementsList", listID + ": Reached end of file early (must have empty line after top-level declarations");
            return;
        }
        lines.remove(0);

        Map<String, RequirementsListStatement> variables = new HashMap<>();
        while (lines.size() > 0) {
            String currentLine = lines.remove(0);
            if (currentLine.length() == 0)
                continue;
            if (!currentLine.contains(declarationCharacter)) {
                Log.e("RequirementsList", listID + ": Unexpected line: " + currentLine);
                continue;
            }
            String[] comps = currentLine.split(declarationCharacter);
            if (comps.length != 2) {
                Log.e("RequirementsList", listID + ": Must have exactly one occurrence of " + declarationCharacter + " per line");
                continue;
            }

            String statementTitle = null, variableName = null;
            String declaration = comps[0];
            if (declaration.contains(variableDeclarationSeparator)) {
                variableName = undecoratedComponent(declaration.substring(0, declaration.indexOf(variableDeclarationSeparator)));
                statementTitle = undecoratedComponent(declaration.substring(declaration.indexOf(variableDeclarationSeparator) + variableDeclarationSeparator.length()));
            } else {
                variableName = undecoratedComponent(declaration);
            }
            variables.put(variableName, RequirementsListStatement.fromStatement(comps[1], statementTitle));
        }

        List<RequirementsListStatement> reqs = new ArrayList<>();
        for (TopLevelSection section : topLevelSections) {
            if (!variables.containsKey(section.title)) {
                Log.e("RequirementsList", listID + ": Undefined variable: " + section.title);
                return;
            }
            RequirementsListStatement req = variables.get(section.title);
            req.contentDescription = section.description;
            reqs.add(req);
        }

        setRequirements(reqs);
        substituteVariableDefinitions(variables);
    }

    @Override
    public String keyPath() {
        return listID;
    }
}
