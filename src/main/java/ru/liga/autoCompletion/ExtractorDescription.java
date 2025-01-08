package ru.liga.autoCompletion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ExtractorDescription {


    public String getFieldDescription(Field field) {
        return "<html><b>Field:</b> " + field.getName() + "<br>" +
                "<b>Type:</b> " + field.getType().getSimpleName() + "</html>";
    }

    public String getMethodDescription(Method method) {
        Class<?>[] params = method.getParameterTypes();
        StringBuilder description = new StringBuilder();
        description.append("<html><b>Method:</b> ").append(method.getName()).append("<br>");
        description.append("<b>Returns:</b> ").append(method.getReturnType().getSimpleName()).append("<br>");
        if (method.getParameterCount() > 0) {
            description.append("<b>Parameters:</b><ul>");
            for (Class<?> param : params) {
                description.append("<li>").append(param.getSimpleName()).append("</li>");
            }
            description.append("</ul>");
        } else {
            description.append("<b>Parameters:</b> None");
        }
        description.append("</html>");
        return description.toString();
    }
}
