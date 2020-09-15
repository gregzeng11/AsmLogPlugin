package com.tencent.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project 

class AsmPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def android = project.extensions.findByType(BaseExtension.class)
        project.extensions.create("MethodHook", MethodHookConfig)
        android.registerTransform(new TiwAsmTransform(project))
    }
}