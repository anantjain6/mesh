package com.gentics.mesh.demo;

import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.model.Group;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.Role;
import com.gentics.mesh.core.data.model.Schema;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.model.TagFamily;
import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.impl.SchemaImpl;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.RoleRoot;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.root.UserRoot;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.MicroPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.PropertyType;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.MeshRootService;
import com.gentics.mesh.core.data.service.MeshUserService;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.data.service.RoleService;
import com.gentics.mesh.core.data.service.SchemaService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

@Component
public class DemoDataProvider {

	private static final Logger log = LoggerFactory.getLogger(DemoDataProvider.class);

	public static final String PROJECT_NAME = "dummy";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	private static SecureRandom random = new SecureRandom();

	@Autowired
	private FramedTransactionalGraph fg;

	@Autowired
	private MeshUserService userService;

	@Autowired
	private MeshRootService rootService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private TagService tagService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	protected MeshSpringConfiguration springConfig;

	@Autowired
	private BootstrapInitializer bootstrapInitializer;

	// References to dummy data

	private Language english;

	private Language german;

	private Project project;
	private UserInfo userInfo;

	private MeshRoot root;

	private Map<String, Schema> schemas = new HashMap<>();
	private Map<String, TagFamily> tagFamilies = new HashMap<>();
	private Map<String, MicroPropertyType> microSchemas = new HashMap<>();
	private Map<String, MeshNode> folders = new HashMap<>();
	private Map<String, MeshNode> contents = new HashMap<>();
	private Map<String, Tag> tags = new HashMap<>();
	private Map<String, MeshUser> users = new HashMap<>();
	private Map<String, Role> roles = new HashMap<>();
	private Map<String, Group> groups = new HashMap<>();

	private DemoDataProvider() {
	}

	public void setup(int multiplicator) throws JsonParseException, JsonMappingException, IOException {
		bootstrapInitializer.initMandatoryData();

		schemas.clear();
		tagFamilies.clear();
		microSchemas.clear();
		contents.clear();
		folders.clear();
		tags.clear();
		users.clear();
		roles.clear();
		groups.clear();

		english = languageService.findByLanguageTag("en");
		german = languageService.findByLanguageTag("de");
		root = rootService.findRoot();
		addUserGroupRoleProject(multiplicator);
		// addMicoSchemas();
		addSchemas();
		addTagFamilies();
		addTags();
		addFolderStructure();
		addContents(multiplicator);
		updatePermissions();

		log.info("Nodes:    " + getNodeCount());
		log.info("Folders:  " + folders.size());
		log.info("Contents: " + contents.size());
		log.info("Tags:     " + tags.size());
		log.info("Schemas: " + schemas.size());
		log.info("TagFamilies: " + tagFamilies.size());
		log.info("Users:    " + users.size());
		log.info("Groups:   " + groups.size());
		log.info("Roles:    " + roles.size());
		fg.commit();
	}

	private void addContents(int multiplicator) {

		Schema contentSchema = schemas.get("content");

		for (int i = 0; i < 12 * multiplicator; i++) {
			addContent(folders.get("2014"), "News_2014_" + i, "News " + i + "!", "Neuigkeiten " + i + "!", contentSchema);
		}

		addContent(folders.get("news"), "News Overview", "News Overview", "News Übersicht", contentSchema);

		addContent(folders.get("deals"), "Super Special Deal 2015", "Buy two get nine!", "Kauf zwei und nimm neun mit!", contentSchema);
		for (int i = 0; i < 12 * multiplicator; i++) {
			addContent(folders.get("deals"), "Special Deal June 2015 - " + i, "Buy two get three! " + i, "Kauf zwei und nimm drei mit!" + i,
					contentSchema);
		}

		addContent(folders.get("2015"), "Special News_2014", "News!", "Neuigkeiten!", contentSchema);
		for (int i = 0; i < 12 * multiplicator; i++) {
			addContent(folders.get("2015"), "News_2015_" + i, "News" + i + "!", "Neuigkeiten " + i + "!", contentSchema);
		}

		MeshNode porsche911 = addContent(
				folders.get("products"),
				"Porsche 911",
				"997 is the internal designation for the Porsche 911 model manufactured and sold by German manufacturer Porsche between 2004 (as Model Year 2005) and 2012.",
				"Porsche 997 ist die interne Modellbezeichnung von Porsche für das von 2004 bis Ende 2012 produzierte 911-Modell.", contentSchema);
		porsche911.addTag(tags.get("vehicle"));
		porsche911.addTag(tags.get("car"));

		MeshNode nissanGTR = addContent(
				folders.get("products"),
				"Nissan GT-R",
				"The Nissan GT-R is a 2-door 2+2 sports coupé produced by Nissan and first released in Japan in 2007",
				"Der Nissan GT-R ist ein seit Dezember 2007 produziertes Sportcoupé des japanischen Automobilherstellers Nissan und der Nachfolger des Nissan Skyline GT-R R34.",
				contentSchema);
		nissanGTR.addTag(tags.get("vehicle"));
		nissanGTR.addTag(tags.get("car"));
		nissanGTR.addTag(tags.get("green"));

		MeshNode bmwM3 = addContent(
				folders.get("products"),
				"BMW M3",
				"The BMW M3 (first launched in 1986) is a high-performance version of the BMW 3-Series, developed by BMW's in-house motorsport division, BMW M.",
				"Der BMW M3 ist ein Sportmodell der 3er-Reihe von BMW, das seit Anfang 1986 hergestellt wird. Dabei handelt es sich um ein Fahrzeug, welches von der BMW-Tochterfirma BMW M GmbH entwickelt und anfangs (E30 und E36) auch produziert wurde.",
				contentSchema);
		bmwM3.addTag(tags.get("vehicle"));
		bmwM3.addTag(tags.get("car"));
		bmwM3.addTag(tags.get("blue"));

		MeshNode concorde = addContent(
				folders.get("products"),
				"Concorde",
				"Aérospatiale-BAC Concorde is a turbojet-powered supersonic passenger jet airliner that was in service from 1976 to 2003.",
				"Die Aérospatiale-BAC Concorde 101/102, kurz Concorde (französisch und englisch für Eintracht, Einigkeit), ist ein Überschall-Passagierflugzeug, das von 1976 bis 2003 betrieben wurde.",
				contentSchema);
		concorde.addTag(tags.get("plane"));
		concorde.addTag(tags.get("twinjet"));
		concorde.addTag(tags.get("red"));

		MeshNode boeing737 = addContent(
				folders.get("products"),
				"Boeing 737",
				"The Boeing 737 is a short- to medium-range twinjet narrow-body airliner. Originally developed as a shorter, lower-cost twin-engined airliner derived from Boeing's 707 and 727, the 737 has developed into a family of nine passenger models with a capacity of 85 to 215 passengers.",
				"Die Boeing 737 des US-amerikanischen Flugzeugherstellers Boeing ist die weltweit meistgebaute Familie strahlgetriebener Verkehrsflugzeuge.",
				contentSchema);
		boeing737.addTag(tags.get("plane"));
		boeing737.addTag(tags.get("twinjet"));

		MeshNode a300 = addContent(
				folders.get("products"),
				"Airbus A300",
				"The Airbus A300 is a short- to medium-range wide-body twin-engine jet airliner that was developed and manufactured by Airbus. Released in 1972 as the world's first twin-engined widebody, it was the first product of Airbus Industrie, a consortium of European aerospace manufacturers, now a subsidiary of Airbus Group.",
				"Der Airbus A300 ist das erste zweistrahlige Großraumflugzeug der Welt, produziert vom europäischen Flugzeughersteller Airbus.",
				contentSchema);
		a300.addTag(tags.get("plane"));
		a300.addTag(tags.get("twinjet"));
		a300.addTag(tags.get("red"));

		MeshNode wrangler = addContent(
				folders.get("products"),
				"Jeep Wrangler",
				"The Jeep Wrangler is a compact and mid-size (Wrangler Unlimited models) four-wheel drive off-road and sport utility vehicle (SUV), manufactured by American automaker Chrysler, under its Jeep marque – and currently in its third generation.",
				"Der Jeep Wrangler ist ein Geländewagen des US-amerikanischen Herstellers Jeep innerhalb des Chrysler-Konzerns.", contentSchema);
		wrangler.addTag(tags.get("vehicle"));
		wrangler.addTag(tags.get("jeep"));

		MeshNode volvo = addContent(folders.get("products"), "Volvo B10M",
				"The Volvo B10M was a mid-engined bus and coach chassis manufactured by Volvo between 1978 and 2003.", null, contentSchema);
		volvo.addTag(tags.get("vehicle"));
		volvo.addTag(tags.get("bus"));

		MeshNode hondact90 = addContent(folders.get("products"), "Honda CT90",
				"The Honda CT90 was a small step-through motorcycle manufactured by Honda from 1966 to 1979.", null, contentSchema);
		hondact90.addTag(tags.get("vehicle"));
		hondact90.addTag(tags.get("motorcycle"));

		MeshNode hondaNR = addContent(
				folders.get("products"),
				"Honda NR",
				"The Honda NR (New Racing) was a V-four motorcycle engine series started by Honda in 1979 with the 500cc NR500 Grand Prix racer that used oval pistons.",
				"Die NR750 ist ein Motorrad mit Ovalkolben-Motor des japanischen Motorradherstellers Honda, von dem in den Jahren 1991 und 1992 300 Exemplare gebaut wurden.",
				contentSchema);
		hondaNR.addTag(tags.get("vehicle"));
		hondaNR.addTag(tags.get("motorcycle"));
		hondaNR.addTag(tags.get("green"));

	}

	private void addFolderStructure() {

		MeshNode rootNode = project.getOrCreateRootNode();
		rootNode.setCreator(userInfo.getUser());
		rootNode.addProject(project);

		MeshNode news = addFolder(rootNode, "News", "Neuigkeiten");
		MeshNode news2015 = addFolder(news, "2015", null);
		news2015.addTag(tags.get("car"));
		news2015.addTag(tags.get("bike"));
		news2015.addTag(tags.get("plane"));
		news2015.addTag(tags.get("jeep"));

		MeshNode news2014 = addFolder(news, "2014", null);
		addFolder(news2014, "March", null);

		addFolder(rootNode, "Products", "Produkte");
		addFolder(rootNode, "Deals", "Angebote");

	}

	private void addTags() {

		TagFamily colorTags = tagFamilies.get("colors");
		TagFamily basicTags = tagFamilies.get("basic");

		// Tags for categories
		addTag("Vehicle", basicTags);
		addTag("Car", basicTags);
		addTag("Jeep", basicTags);
		addTag("Bike", basicTags);
		addTag("Motorcycle", basicTags);
		addTag("Bus", basicTags);
		addTag("Plane", basicTags);
		addTag("JetFigther", basicTags);
		addTag("Twinjet", basicTags);

		// Tags for colors
		addTag("red", colorTags);
		addTag("blue", colorTags);
		addTag("green", colorTags);

	}

	public UserInfo createUserInfo(String username, String firstname, String lastname) {

		String password = "test123";
		String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";

		MeshUser user = root.getUserRoot().create(username);
		user.setUuid("UUIDOFUSER1");
		user.setPassword(password);
		log.info("Creating user with username: " + username + " and password: " + password);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setEmailAddress(email);
		users.put(username, user);

		String roleName = username + "_role";
		Role role = root.getRoleRoot().create(roleName);

		role.addPermissions(role, READ_PERM);
		roles.put(roleName, role);

		String groupName = username + "_group";
		Group group = root.getGroupRoot().create(groupName);
		group.addUser(user);
		group.addRole(role);
		groups.put(groupName, group);

		UserInfo userInfo = new UserInfo(user, group, role, password);
		return userInfo;

	}

	private void addUserGroupRoleProject(int multiplicator) {
		// User, Groups, Roles
		userInfo = createUserInfo("joe1", "Joe", "Doe");
		UserRoot userRoot = getMeshRoot().getUserRoot();
		GroupRoot groupRoot = getMeshRoot().getGroupRoot();
		RoleRoot roleRoot = getMeshRoot().getRoleRoot();

		project = root.getProjectRoot().create(PROJECT_NAME);
		project.setCreator(userInfo.getUser());

		// Guest Group / Role
		Role guestRole = root.getRoleRoot().create("guest_role");
		roles.put(guestRole.getName(), guestRole);

		Group guests = root.getGroupRoot().create("guests");
		guests.addRole(guestRole);
		groups.put("guests", guests);

		// Extra User
		for (int i = 0; i < 12 * multiplicator; i++) {
			MeshUser user = userRoot.create("guest_" + i);
			// userService.setPassword(user, "guestpw" + i);
			user.setFirstname("Guest Firstname");
			user.setLastname("Guest Lastname");
			user.setEmailAddress("guest_" + i + "@spam.gentics.com");
			guests.addUser(user);
			users.put(user.getUsername(), user);
		}
		// Extra Groups
		for (int i = 0; i < 12 * multiplicator; i++) {
			Group group = groupRoot.create("extra_group_" + i);
			groups.put(group.getName(), group);
		}

		// Extra Roles
		for (int i = 0; i < 12 * multiplicator; i++) {
			Role role = roleRoot.create("extra_role_" + i);
			roles.put(role.getName(), role);
		}
	}

	// private void addMicoSchemas() {
	// SchemaRoot schemaRoot = root.getSchemaRoot();
	// MicroPropertyType imageGallery = schemaService.createMicroPropertyTypeSchema("gallery");
	// BasicPropertyType descriptionSchema = imageGallery.createBasicPropertyTypeSchema("description", PropertyType.STRING);
	// imageGallery.addProperty(descriptionSchema);
	//
	// BasicPropertyType imagesSchemas = imageGallery.createListPropertyTypeSchema("images");
	// // imagesSchemas.add(PropertyType.REFERENCE);
	// imageGallery.addProperty(imagesSchemas);
	// microSchemas.put("gallery", imageGallery);
	//
	// }

	private void addTagFamilies() {
		TagFamily basicTagFamily = getProject().getTagFamilyRoot().create("basic");
		basicTagFamily.setDescription("Description for basic tag family");
		tagFamilies.put("basic", basicTagFamily);

		TagFamily colorTagFamily = getProject().getTagFamilyRoot().create("colors");
		basicTagFamily.setDescription("Description for color tag family");
		tagFamilies.put("colors", colorTagFamily);
	}

	private void addSchemas() {
		addBootstrapSchemas();
		addBlogPostSchema();
		// addColorsSchema();
		addCategorySchema();
	}

	private void addBootstrapSchemas() {

		// folder
		Schema folderSchema = schemaService.findByName("folder");
		folderSchema.addProject(project);
		schemas.put("folder", folderSchema);

		// content
		Schema contentSchema = schemaService.findByName("content");
		contentSchema.addProject(project);
		schemas.put("content", contentSchema);

		// binary-content
		Schema binaryContentSchema = schemaService.findByName("binary-content");
		binaryContentSchema.addProject(project);
		schemas.put("binary-content", binaryContentSchema);

	}

	// private void addColorsSchema() {
	// SchemaRoot schemaRoot = root.getSchemaRoot();
	// Schema colorSchema = schemaRoot.create("colors");
	// colorSchema.setDescription("Colors");
	// colorSchema.setDescription("Colors");
	// BasicPropertyType nameProp = colorSchema.createBasicPropertyTypeSchema(Schema.NAME_KEYWORD, PropertyType.I18N_STRING);
	// nameProp.setDisplayName("Name");
	// nameProp.setDescription("The name of the category.");
	// colorSchema.addPropertyTypeSchema(nameProp);
	// schemas.put("color", colorSchema);
	// }

	private void addBlogPostSchema() {
		SchemaRoot schemaRoot = root.getSchemaRoot();
		Schema blogPostSchema = schemaRoot.create("blogpost");
		BasicPropertyType content = blogPostSchema.createBasicPropertyTypeSchema("content", PropertyType.LIST);
		blogPostSchema.addPropertyTypeSchema(content);
		schemas.put("blogpost", blogPostSchema);

	}

	private void addCategorySchema() {
		SchemaRoot root = getMeshRoot().getSchemaRoot();
		Schema categoriesSchema = root.create(TAG_CATEGORIES_SCHEMA_NAME);
		categoriesSchema.addProject(project);
		categoriesSchema.setDisplayName("Category");
		categoriesSchema.setDescription("Custom schema for tag categories");
		categoriesSchema.setCreator(userInfo.getUser());
		BasicPropertyType nameProp = categoriesSchema.createBasicPropertyTypeSchema(SchemaImpl.NAME_KEYWORD, PropertyType.I18N_STRING);
		nameProp.setDisplayName("Name");
		nameProp.setDescription("The name of the category.");
		categoriesSchema.addPropertyTypeSchema(nameProp);

		BasicPropertyType displayNameProp = categoriesSchema.createBasicPropertyTypeSchema(SchemaImpl.DISPLAY_NAME_KEYWORD, PropertyType.I18N_STRING);
		displayNameProp.setDisplayName("Display Name");
		displayNameProp.setDescription("The display name property of the category.");
		categoriesSchema.addPropertyTypeSchema(displayNameProp);

		BasicPropertyType contentProp = categoriesSchema.createBasicPropertyTypeSchema(SchemaImpl.CONTENT_KEYWORD, PropertyType.I18N_STRING);
		contentProp.setDisplayName("Content");
		contentProp.setDescription("The main content html of the category.");
		categoriesSchema.addPropertyTypeSchema(contentProp);
		schemas.put("category", categoriesSchema);

	}

	private void updatePermissions() {
		// // Add Permissions
		// // Add admin permissions to all nodes
		// int i = 0;
		// for (GenericNode currentNode : genericNodeService.findAll()) {
		// currentNode = genericNodeService.reload(currentNode);
		// log.info("Adding BasicPermission to node {" + currentNode.getId() + "}");
		// if (adminRole.getId() == currentNode.getId()) {
		// log.info("Skipping role");
		// continue;
		// }
		// roleService.addPermission(adminRole, currentNode, CREATE, READ, UPDATE, DELETE);
		// adminRole = roleService.save(adminRole);
		// log.info("Added permissions to {" + i + "} objects.");
		// i++;
		// }

		// TODO determine why this is not working when using sdn
		// Add Permissions
		// Node roleNode = neo4jTemplate.getPersistentState(userInfo.getRole());
		Role role = userInfo.getRole();

		for (Vertex vertex : fg.getVertices()) {
			WrappedVertex wrappedVertex = (WrappedVertex) vertex;

			// TODO typecheck? and verify how orient will behave
			if (role.getUuid().equalsIgnoreCase(vertex.getProperty("uuid"))) {
				log.info("Skipping own role");
				continue;
			}

			MeshVertex meshVertex = fg.frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
			role.addPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM);

			// GraphPermission perm = role.addPermissions();
			// perm.setProperty("permissions-read", true);
			// perm.setProperty("permissions-delete", true);
			// perm.setProperty("permissions-create", true);
			// perm.setProperty("permissions-update", true);
			// GenericNode sdnNode = neo4jTemplate.projectTo(node, GenericNode.class);
			// roleService.addPermission(adminRole, sdnNode, CREATE, READ, UPDATE, DELETE);
			// genericNodeService.save(node);

		}
		log.info("Added BasicPermissions to nodes");

	}

	public MeshNode addFolder(MeshNode rootNode, String englishName, String germanName) {
		MeshNode folderNode = rootNode.create();
		folderNode.setParentNode(rootNode);
		folderNode.addProject(project);

		if (germanName != null) {
			MeshNodeFieldContainer germanContainer = folderNode.getOrCreateFieldContainer(german);
			germanContainer.setI18nProperty("displayName", germanName);
			germanContainer.setI18nProperty("name", germanName);
		}
		if (englishName != null) {
			MeshNodeFieldContainer englishContainer = folderNode.getOrCreateFieldContainer(english);
			englishContainer.setI18nProperty("displayName", englishName);
			englishContainer.setI18nProperty("name", englishName);
		}
		folderNode.setCreator(userInfo.getUser());
		folderNode.setSchema(schemas.get("folder"));
		if (englishName == null || StringUtils.isEmpty(englishName)) {
			throw new RuntimeException("Key for folder empty");
		}
		if (folders.containsKey(englishName.toLowerCase())) {
			throw new RuntimeException("Collision of folders detected for key " + englishName.toLowerCase());
		}

		folders.put(englishName.toLowerCase(), folderNode);
		return folderNode;
	}

	public Tag addTag(String name) {
		return addTag(name, getTagFamily("demo"));
	}

	public Tag addTag(String name, TagFamily tagFamily) {
		if (name == null || StringUtils.isEmpty(name)) {
			throw new RuntimeException("Name for tag empty");
		}
		Tag tag = tagFamily.create(name);
		tag.addProject(project);
		tag.setCreator(userInfo.getUser());
		tags.put(name.toLowerCase(), tag);
		return tag;
	}

	private MeshNode addContent(MeshNode parentNode, String name, String englishContent, String germanContent, Schema schema) {
		MeshNode node = parentNode.create();
		MeshNodeFieldContainer englishContainer = node.getOrCreateFieldContainer(english);
		englishContainer.setI18nProperty("displayName", name + " english");
		englishContainer.setI18nProperty("name", name + ".en.html");
		englishContainer.setI18nProperty("content", englishContent);

		if (germanContent != null) {
			MeshNodeFieldContainer germanContainer = node.getOrCreateFieldContainer(german);
			germanContainer.setI18nProperty("displayName", name + " german");
			germanContainer.setI18nProperty("name", name + ".de.html");
			germanContainer.setI18nProperty("content", germanContent);
		}
		// TODO maybe set project should be done inside the save?
		node.addProject(project);
		node.setCreator(userInfo.getUser());
		node.setSchema(schema);
		// node.setOrder(42);
		node.setParentNode(parentNode);
		// Add the content to the given tag
		// parentTag.addContent(content);
		// parentTag = tagService.save(parentTag);

		if (contents.containsKey(name.toLowerCase())) {
			throw new RuntimeException("Collsion of contents detected for key " + name.toLowerCase());
		}
		contents.put(name.toLowerCase(), node);
		return node;
	}

	/**
	 * Returns the path to the tag for the given language.
	 */
	public String getPathForNews2015Tag(Language language) {

		String name = folders.get("news").getFieldContainer(language).getI18nProperty("name");
		String name2 = folders.get("2015").getFieldContainer(language).getI18nProperty("name");
		return name + "/" + name2;
	}

	public Language getEnglish() {
		return english;
	}

	public Language getGerman() {
		return german;
	}

	public Project getProject() {
		return project;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public MeshNode getFolder(String name) {
		return folders.get(name);
	}

	public TagFamily getTagFamily(String key) {
		return tagFamilies.get(key);
	}

	public MeshNode getContent(String name) {
		return contents.get(name);
	}

	public Tag getTag(String name) {
		return tags.get(name);
	}

	public Schema getSchema(String name) {
		return schemas.get(name);
	}

	public Map<String, Tag> getTags() {
		return tags;
	}

	public Map<String, MeshNode> getContents() {
		return contents;
	}

	public Map<String, MeshNode> getFolders() {
		return folders;
	}

	public Map<String, MeshUser> getUsers() {
		return users;
	}

	public Map<String, Group> getGroups() {
		return groups;
	}

	public Map<String, Role> getRoles() {
		return roles;
	}

	public Map<String, Schema> getSchemas() {
		return schemas;
	}

	public MeshRoot getMeshRoot() {
		return root;
	}

	public int getNodeCount() {
		return folders.size() + contents.size() + root.getProjectRoot().getProjects().size();
	}

}
