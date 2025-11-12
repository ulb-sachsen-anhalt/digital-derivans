# Digital Derivans

![JDK11 Maven3](https://github.com/ulb-sachsen-anhalt/digital-derivans/workflows/Java%20CI%20with%20Maven/badge.svg)

## What is the tool used for?

This java command line tool can be used to generate PDF files from image derivatives with configurable scales, qualities, and additional asset files such as preview images or thumbnails. 
If full text is available (structured OCR formats like ALTO or PAGE), this can also be added. This makes the information contained in the documents usable, searchable, and efficiently manageable.

## The tool enables:

* Efficient information retrieval: Instead of laboriously searching through hundreds or thousands of pages manually, you can search for specific words, phrases, names, or data with a single click and locate them immediately. This saves considerable time and effort, especially with large data sets.
* Improved accessibility and management: Searchable PDFs transform static images (pure scans) into dynamic sources of information. They can be seamlessly integrated into digital document management systems (DMS), simplifying the organization, categorization, and retrieval of documents.
* Text editing and extraction: OCR technology converts the text in the image into machine-readable, editable text. This allows you to copy, highlight, edit, or export text passages to other applications without having to type them out manually.
* Long-term usability and archiving: Conversion to searchable formats such as PDF/A (a standard for long-term archiving) ensures that documents remain readable and usable in the future, regardless of technological changes.
* Accessibility: Searchable PDFs improve accessibility for visually impaired people, as screen readers can capture and read aloud the machine-readable text.
  

* [Features](#features)
* [Requirements](#requirements)
* [Installation](#installation)
* [Usage](#usage)
* [Configuration](#configuration)
* [Limitations](#limitations)
* [License](#license)

## Features

The tool creates a PDF from scaled image data (e.g., jpeg, tiff) with settings for compression rate and max sizes.  
Additional features: 
* OCR textlayer
* metadata (METS/MODS)
* outline of contents
* footer
* preview image
* thumbnail
    
For details see [configuration section](#configuration).

If metadata (METS/MODS) available, the following will be taken into account:

* Value `mods:recordIdentifier[@source]` to name PDF artefact
  Can be replaced in PDF-step configuration with option `mods_identifier_xpath` or at execution time via CLI-parameter `-n` / `--name-pdf`
* Value `mods:titleInfo/mods:title` is used for internal naming
* Attribute `mets:div[@CONTENTIDS]` (granular URN) will be rendered for each page if footer shall be appended to each page image

Please note: Derivans does not perform metadata validation or sanitizing.
If METS/MODS are provided, the process fails for empty logical sections — i.e. chapters that are not linked to any image or page — because a consistent outline cannot be created in this case. To ensure XML conformance before/after using Derivans, one needs to use a different tool.

## Requirements
Digital Derivans is a Java 11+ project build with [Apache Maven](https://github.com/apache/maven).
  * CLI: A command line interface (CLI), which allows you to enter text commands to configure, navigate, or run programs on your computer system, is needed. All operating systems—including Linux, macOS, and Windows—offer such a CLI for faster system interaction.
  * OpenJDK 11+: OpenJDK is a free, open-source implementation of the Java platform (Standard Edition).
  * Maven 3.6+: Maven is a software project management and comprehension tool. Based on the concept of a project object model (POM), Maven can manage a project's build, reporting, and documentation from a central place.
  * Git 2.12+: Git is a distributed version control software system that is capable of managing versions of source code or data.

## Installation
The tool can be installed in two ways:
* via docker image
* full local installation
    
### Docker Image

Pull the [Docker image](https://github.com/ulb-sachsen-anhalt/digital-derivans/pkgs/container/digital-derivansout):

```bash
docker pull ghcr.io/ulb-sachsen-anhalt/digital-derivans:latest
```

or build your own locally:

```bash
./scripts/build_docker_image.sh
```

Usage of docker image is described in [Usage](#usage) section, but all required directories / files need to be passed as mapped volumes.

For example:

```bash
docker run \
  --mount type=bind,source=<host-work-dir>,target=/data-print \
  --mount type=bind,source=<host-config-dir>,target=/data-config \
  --mount type=bind,source=<host-log-dir>,target=/data-log \
  ghcr.io/ulb-sachsen-anhalt/digital-derivans \ 
  <print-dir|mets-file> -c /data-config/derivans.ini  
```

### Local Installation

Open the command line tool.

Execute the following command:

```shell
git clone git@github.com:ulb-sachsen-anhalt/digital-derivans.git
```
The console output should resemble the following:
<pre>Klone nach &apos;digital-derivans&apos; …
remote: Enumerating objects: 6478, done.
remote: Counting objects: 100% (340/340), done.
remote: Compressing objects: 100% (163/163), done.
remote: Total 6478 (delta 139), reused 252 (delta 108), pack-reused 6138 (from 2)
Empfange Objekte: 100% (6478/6478), 38.43 MiB | 675.00 KiB/s, fertig.
Löse Unterschiede auf: 100% (2969/2969), fertig.</pre>

The folder ‚digital-derivans‘ is created automatically.

Change to folder using the command ``cd digital-derivans/``

Execute the command: ``mvn clean package`` (mvn stands for Maven. This is required for automating the build process for Java projects.)

  * If Maven is not available, the following error message is displayed: 

    Der Befehl 'mvn' wurde nicht gefunden, kann aber installiert werden mit:
    ``sudo apt install maven
    ``
    Executing the command will install Maven. (You may need to enter your local PC password here.) This is followed by a long list of the installed packages.
    Then execute the `mvn clean package` command again.
    
    This is followed by a long list of downloads, which should end as follows:

<pre>[<font color="#12488B"><b>INFO</b></font>] <b>------------------------------------------------------------------------</b>
[<font color="#12488B"><b>INFO</b></font>] <font color="#26A269"><b>BUILD SUCCESS</b></font>
[<font color="#12488B"><b>INFO</b></font>] <b>------------------------------------------------------------------------</b>
[<font color="#12488B"><b>INFO</b></font>] Total time:  22.808 s
[<font color="#12488B"><b>INFO</b></font>] Finished at: 2025-11-11T13:17:12+01:00
[<font color="#12488B"><b>INFO</b></font>] <b>------------------------------------------------------------------------</b></pre>

A shaded JAR (`./target/digital-derivans-<version>.jar`) is created in the ‚digital-derivans‘ folder. This shaded jar, also known as an Uber jar or fat jar, contains all the dependencies required to run the Java application by default.

This completes the installation.

## Usage

To use the tool locally, a recent version of OpenJRE (Java Runtime Environment) is required.

In addition, a specific folder structure must be adhered to. The tool expects a project folder (here: `my_print`) containing an image directory (here: `DEFAULT`), which contains the image files, and an optional OCR-data directory (here: `FULLTEXT`), which contains OCR-data in XML format, if available. The naming of the image and OCR-data files must be consistent to ensure error-free assignment.

A sample folder structure:

```bash
my_print/
├── FULLTEXT
│   ├── 0002.xml
│   ├── 0021.xml
│   ├── 0332.xml
├── DEFAULT
│   ├── 0002.tif
│   ├── 0021.tif
│   ├── 0332.tif
```

The *default name* of the generated PDF inside is derived from the object's folder name or can be set with `-n`-arg.

To run the tool, enter the following command:

```bash
java -jar <PATH>./target/digital-derivans-<version>.jar <path-to-my_print>
```
The file path to the .jar must be specified, as well as the version (e.g., 2.1.3.jar) and the file path to the parent folder (here: my_print), where the subfolders with the image and OCR files are located.

A file named `my_print.pdf` is created in the `my_print` directory from the above example with the specified layout.
The generated PDF contains the full text, which is invisible above the image layer. The page is searchable and text passages can be highlighted.

For more information concerning CLI-Usage, please [consult CLI docs](#cli-parameter).

## Configuration

Although Derivans can be run without configuration, it's strongly recommended. Many flags, especially if metadata must
be taken into account, are using defaults tied to digitization workflows of ULB Sachsen-Anhalt that *might* not fit
your custom requirements.

### Configure Sections

Configuration options can be bundled into sections and customized with a INI-file.

Some params can be set on global level, like quality and poolsize.  
Each section in a `*.ini`- file matching `[derivate_<n>]` represents a single derivate section for intermediate or final
derivates.

Order of execution is determined by numbering of derivate sections at parse-time.

### Default Values

On top of the INI-file are global configuration options listed, which will be used as defaults for actual steps, if they can be
applied.

* `default_quality`  : image data compression rate (can be specified with `quality` for image derivate sections)
* `default_poolsize` : poolsize of worker threads for parallel processing (can be specified with `poolsize` for image
  derivate sections)

### Section-specific Configuration

Some options values must be set individually for each step:

* `input_dir` : path to directory with section input files
* `output_dir`: path to directory for section output

Additional options can be set, according to of the actual type to derive:

Images:

* `quality` : compression rate
* `poolsize` : parallel workers
* `maximal` : maximal dimension (affects both width and height)
* `footer_template` : footer template Path
* `footer_label_copyright` : additional (static) label for footer

PDF:

* `metadata_creator` : enrich creator tag
* `metadata_keywords`: enrich keywords
* `enrich_pdf_metadata` : if PDF shall be enriched into METS/MODS (default: `True`)
* `mods_identifier_xpath` : if not set, use `mods:recordIdentifier` from primary MODS

### Minimal working Example

The following example configuration contains global settings and subsequent generation steps.  
(Example directory and file layout like from [Usage section](#usage) assumed.)

On global level, it sets the default JPEG-quality to `75`, the number of parallel executors to `4` (recommended if at
least 4 CPUs available) and determines the file for the logging-configuration.

1. Create JPEG images from images in sub directory `MAX` with compression rate 75, scale to maximal dimension 1000px and store
   in sub dir `IMAGE_75`.
2. Create PDF with images from `IMAGE_75`, add some PDF metadata and store file as `my_print.pdf` in current dir.

```ini
default_quality = 75
default_poolsize = 4
logger_configuration_file = derivans_logging.xml

[derivate_01]
input_dir = MAX
output_dir = IMAGE_75
maximal = 1000

[derivate_02]
type = pdf
input_dir = IMAGE_75
output_dir = .
output_type = pdf
metadata_creator = "<your organization label>"
metadata_license = "<your licence>"
```

### CLI Parameter

The main parameter for Derivans is the input path, which may be a local directory in local mode or the path to a local
METS/MODS-file with sub directories for images and OCR-data, if using metadata.

Additionally, one can also provide via CLI

* `-c` path to custom configuration INI-file
* `-d` flag to turn on rendering of boxes and text if using OCR input
* `-n` set custom name for resulting PDF
* set labels for OCR and input-image (will overwrite configuration)  
  If metadata present, both will be used as filegroup names;
  For images they will also be used as input directory for initial image processing

## Limitations

Derivans depends on standard JDK11-components and external components for image processing and PDF generation.

### Step Configuration

* Subsequent derivate steps must not have order gaps, since the parsing is done step by step. Otherwise, any derivate
  section after the first gap will be ignored, which may lead to unexpected results.

### Image Processing

*Please note*:  
To overcome `javax.imageio` errors, it's recommended to fix them using an external image processing application.

* Images with more than 8bit channel depth can't be processed  [javax.imageio.IIOException: Illegal band size](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/42)
* Uncommon image metadata can't be processed  
[javax.imageio.IIOException: Unsupported marker](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/33)
* Integral dimension values required for proper scaling  [javax.imageio.metadata.IIOInvalidTreeException: Xdensity attribute out of range](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/53)

### PDF Generation

* If Derivans is called from within the project folder, the resulting pdf will be called `..pdf`.
* iText PDF-Library limits the maximal page dimension to 14400 px (
  weight/height, [Configured max dimension fails for very large Images](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/16)).
  This may cause trouble if one needs to generate PDF for very large prints like maps, deeds or scrolls.

### Metadata

* Derivans [does not accept METS with current OCR-D-style](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/38)
nor any other METS which contains extended XML-features like inline namespace declarations.

## License

This project's source code is licensed under terms of the [MIT license](https://opensource.org/licenses/MIT).

NOTE: This project depends on components that may use different license terms.
