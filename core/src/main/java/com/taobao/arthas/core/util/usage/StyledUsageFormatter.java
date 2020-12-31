package com.taobao.arthas.core.util.usage;

import com.taobao.middleware.cli.Argument;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.UsageMessageFormatter;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;


import java.util.Collections;

import static com.taobao.text.ui.Element.row;
import static com.taobao.text.ui.Element.label;

/**
 * @author ralf0131 2016-12-14 22:16.
 */
public class StyledUsageFormatter extends UsageMessageFormatter {

    private Color fontColor;

    public StyledUsageFormatter(Color fontColor) {
        this.fontColor = fontColor;
    }

    public static String styledUsage(CLI cli, int width) {
        if(cli == null) {
            return "";
        }
        StringBuilder usageBuilder = new StringBuilder();
        UsageMessageFormatter formatter = new StyledUsageFormatter(Color.green);
        formatter.setWidth(width);
        cli.usage(usageBuilder, formatter);
        return usageBuilder.toString();
    }

    @Override
    public void usage(StringBuilder builder, String prefix, CLI cli) {

        TableElement table = new TableElement(1, 2).leftCellPadding(1).rightCellPadding(1);

        table.add(row().add(label("USAGE:").style(getHighlightedStyle())));
        table.add(row().add(label(computeUsageLine(prefix, cli))));
        table.add(row().add(""));
        table.add(row().add(label("SUMMARY:").style(getHighlightedStyle())));
        table.add(row().add(label("  " + cli.getSummary())));

        if (cli.getDescription() != null) {
            String[] descLines = cli.getDescription().split("\\n");
            for (String line: descLines) {
                if (shouldBeHighlighted(line)) {
                    table.add(row().add(label(line).style(getHighlightedStyle())));
                } else {
                    table.add(row().add(label(line)));
                }
            }
        }

        if (!cli.getOptions().isEmpty() || !cli.getArguments().isEmpty()) {
            table.add(row().add(""));
            table.row(label("OPTIONS:").style(getHighlightedStyle()));
            for (Option option : cli.getOptions()) {
                StringBuilder optionSb = new StringBuilder(32);

                // short name
                if (isNullOrEmpty(option.getShortName())) {
                    optionSb.append("   ");
                } else {
                    optionSb.append('-').append(option.getShortName());
                    if (isNullOrEmpty(option.getLongName())) {
                        optionSb.append(' ');
                    } else {
                        optionSb.append(',');
                    }
                }
                // long name
                if (!isNullOrEmpty(option.getLongName())) {
                    optionSb.append(" --").append(option.getLongName());
                }

                if (option.acceptValue()) {
                    optionSb.append(" <value>");
                }

                table.add(row().add(label(optionSb.toString()).style(getHighlightedStyle()))
                                .add(option.getDescription()));
            }

            for (Argument argument: cli.getArguments()) {
                table.add(row().add(label("<" + argument.getArgName() + ">").style(getHighlightedStyle()))
                        .add(argument.getDescription()));
            }
        }

        builder.append(RenderUtil.render(table, getWidth()));
    }

    private Style.Composite getHighlightedStyle() {
        return Style.style(Decoration.bold, fontColor);
    }

    public String computeUsageLine(String prefix, CLI cli) {
        // initialise the string buffer
        StringBuilder buff;
        if (prefix == null) {
            buff = new StringBuilder("  ");
        } else {
            buff = new StringBuilder("  ").append(prefix);
            if (!prefix.endsWith(" ")) {
                buff.append(" ");
            }
        }

        buff.append(cli.getName()).append(" ");

        if (getOptionComparator() != null) {
            Collections.sort(cli.getOptions(), getOptionComparator());
        }

        // iterate over the options
        for (Option option : cli.getOptions()) {
            appendOption(buff, option);
            buff.append(" ");
        }

        // iterate over the arguments
        for (Argument arg : cli.getArguments()) {
            appendArgument(buff, arg, arg.isRequired());
            buff.append(" ");
        }

        return buff.toString();
    }

    private boolean shouldBeHighlighted(String line) {
        return !line.startsWith(" ");
    }

}
