package com.example.m.kulka2d;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by rbisioro on 1/19/2018.
 */

class ShaderProgram {
    public int programHandle;

    // Adresy zmiennych uniform w shaderach.
    public int _MVPMatrixHandle=-1;
    public int _MVMatrixHandle=-1;
    public int _diffuseTextureHandle =-1;

    // Adresy atrybutów w vertex shaderze.
    public int _vertexPositionHandle=-1;
    public int _vertexColourHandle=-1;
    public int _vertexNormalHandle=-1;
    public int _vertexTexCoordHandle=-1;

    public void init(int vertexShaderId, int fragmentShaderId, String[] attributeBindings, Context appContext, String debugName)
    {
        programHandle = createShaders(vertexShaderId, fragmentShaderId, attributeBindings, appContext, debugName);
        getVariableHandles();
    }

    public void getVariableHandles()
    {
        Log.d("KSG", "Pobranie adresów zmiennych w shaderach.");
        _MVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "MVPMatrix");
        _MVMatrixHandle = GLES20.glGetUniformLocation(programHandle, "MVMatrix");

        _vertexPositionHandle = GLES20.glGetAttribLocation(programHandle, "vertexPosition");
        _vertexColourHandle = GLES20.glGetAttribLocation(programHandle, "vertexColour");
        _vertexNormalHandle = GLES20.glGetAttribLocation(programHandle, "vertexNormal");
        _vertexTexCoordHandle = GLES20.glGetAttribLocation(programHandle, "vertexTexCoord");
        _diffuseTextureHandle = GLES20.glGetUniformLocation(programHandle, "diffuseTexture");
    }

    protected int createShaders(int vertexShaderId, int fragmentShaderId, String[] attributeBindings, Context appContext, String debugName)
    {
        Log.d("KSG", "Przygotowanie shaderów ("+debugName+").");

        Log.d("KSG", " Wczytywanie vertex shadera.");
        final String vertexShader = readShaderFile(vertexShaderId, appContext);
        if (vertexShader == null)
        {
            Log.e("KSG", "Plik vertex shadera nie został wczytany.");
        }

        Log.d("KSG", " Wczytywanie fragment shadera.");
        final String fragmentShader = readShaderFile(fragmentShaderId, appContext);
        if (fragmentShader == null)
        {
            Log.e("KSG", "Plik fragment shadera nie został wczytany.");
        }

        Log.d("KSG", " Kompilacja vertex shadera.");
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (vertexShaderHandle == 0)
        {
            Log.e("KSG", "Nie można utworzyć vertex shadera.");
            return -1;
        }
        else
        {
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);
            GLES20.glCompileShader(vertexShaderHandle);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0)
            {
                Log.e("KSG", "Błąd kompilacji vertex shadera.");
                Log.e("KSG", GLES20.glGetShaderInfoLog(vertexShaderHandle));
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
                return -1;
            }
        }

        Log.d("KSG", " Kompilacja fragment shadera.");
        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (fragmentShaderHandle == 0)
        {
            Log.e("KSG", "Nie można utworzyć fragment shadera.");
            return -1;
        }
        else
        {
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);
            GLES20.glCompileShader(fragmentShaderHandle);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0)
            {
                Log.e("KSG", "Błąd kompilacji fragment shadera.");
                Log.e("KSG", GLES20.glGetShaderInfoLog(fragmentShaderHandle));
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
                return -1;
            }
        }

        Log.d("KSG", " Linkowanie shaderów.");
        int programHandle = GLES20.glCreateProgram();
        if (programHandle == 0)
        {
            Log.e("KSG", "Nie można podlinkować shaderów.");
            return -1;
        }
        else
        {
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            String tempAttributes = "";
            for (int i=0; i<attributeBindings.length; i++)
            {
                GLES20.glBindAttribLocation(programHandle, i, attributeBindings[i]);
                tempAttributes += " " + attributeBindings[i];
            }
            Log.d("KSG", " shader attributes:" +tempAttributes);
            GLES20.glBindAttribLocation(programHandle, 0, "vertexPosition");
            GLES20.glBindAttribLocation(programHandle, 1, "vertexColour");
            GLES20.glBindAttribLocation(programHandle, 2, "vertexNormal");

            GLES20.glLinkProgram(programHandle);
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0)
            {
                Log.e("KSG", "Błąd linkowania shaderów.");
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
                return -1;
            }
        }

        // Wykorzystanie utworzonych shaderów podczas rysowania.
        GLES20.glUseProgram(programHandle);
        return programHandle;
    }

    // Metoda wczytująca kod shadera z katalogu raw.
    public String readShaderFile(int resourceId, Context appContext)
    {
        if (appContext == null)
        {
            Log.e("KSG", "readShaderFile: appContext == null");
            return null;
        }

        InputStream inputStream = appContext.getResources().openRawResource(resourceId);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = "";
        StringBuilder sb = new StringBuilder();

        try
        {
            while ((line = bufferedReader.readLine()) != null)
            {
                sb.append(line);
                sb.append('\n');
                //Log.d("KSG", line);
            }
        }
        catch (Exception e)
        {
            Log.e("KSG", "Błąd przy wczytywaniu pliku: " + e.getMessage());
            return null;
        }

        return sb.toString();
    }
}

