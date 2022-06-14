# Templates

Find all provided documentation templates in this folder. Please note that the _italic text 
and sentences_ should be removed. Feel free to add additional sections and subsections, however, make sure
that at least the sections of the templates marked as "mandatory" are filled.

## Decision Records

[Link](decision-record.md) to template.

Each decision record should be put in an appropriate folder that is following a naming pattern: 
`YYYY-MM-DD-title-of-decision-record`. This should be located at the [decision record folder](../developer/decision-records) 
and contain all relevant files, at least a filled-out template named `README.md` and any additional images.

As of now, every merged decision record is in state `accepted`. Please make sure to add a comment to
a decision record that replaces a previous one with adding a hint: `superseded by [...]`.

## Extensions

[Link](extension.md) to template.

Every module located [in the extensions folder](../../extensions) has to provide documentation regarding its 
functionality, implementation, or architectural details.
The filled-out template has to be added as `README.md` to each module. Any additional files can be placed 
in a dedicated `docs` directory. As defined by the template, this markdown file can point to submodules 
that provide the same documentation scope themselves.

## Launchers

[Link](launcher.md) to template.

Every module located [in the launchers folder](../../launchers) has to provide documentation regarding its purpose and usage.
The filled template has to be added as `README.md` to each module. Any additional files can be placed 
in a dedicated `docs` directory.
