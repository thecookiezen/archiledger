package com.thecookiezen.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.data.annotation.Id;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.thecookiezen.ladybugdb.spring.annotation.NodeEntity;
import com.thecookiezen.ladybugdb.spring.annotation.RelationshipEntity;
import com.thecookiezen.ladybugdb.spring.connection.SimpleConnectionFactory;
import com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate;
import com.thecookiezen.ladybugdb.spring.mapper.EntityWriter;
import com.thecookiezen.ladybugdb.spring.mapper.RowMapper;
import com.thecookiezen.ladybugdb.spring.mapper.ValueMappers;
import com.thecookiezen.ladybugdb.spring.repository.NodeRepository;
import com.thecookiezen.ladybugdb.spring.repository.support.EntityRegistry;
import com.thecookiezen.ladybugdb.spring.repository.support.LadybugDBRepositoryFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Fork(value = 2)
@Warmup(iterations = 2)
@Measurement(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class LadybugdbBenchmark {

    private Database db;
    private SimpleConnectionFactory connectionFactory;
    private LadybugDBTemplate template;
    private TestRepository repository;
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Setup(Level.Trial)
    public void setup() throws Exception {
        db = new Database(":memory:");
        connectionFactory = new SimpleConnectionFactory(db);
        template = new LadybugDBTemplate(connectionFactory);

        EntityRegistry registry = new EntityRegistry();
        registry.registerDescriptor(Person.class, personReader, personWriter);
        registry.registerDescriptor(Follows.class, followsReader, followsWriter);

        LadybugDBRepositoryFactory factory = new LadybugDBRepositoryFactory(template, registry);
        repository = factory.getRepository(TestRepository.class);

        try (Connection conn = new Connection(db)) {
            conn.query("CREATE NODE TABLE Person(name STRING PRIMARY KEY, age INT64)");
            conn.query("CREATE REL TABLE FOLLOWS(FROM Person TO Person, name STRING, since INT64)");

            for (int i = 0; i < 1000; i++) {
                conn.query("CREATE (:Person {name: 'User" + i + "', age: " + i + "})");
            }
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        connectionFactory.close();
        db.close();
    }

    @Benchmark
    public void createNode(Blackhole bh) {
        String random = "User" + idGenerator.incrementAndGet();
        int randomAge = (int) (Math.random() * 100);
        repository.save(new Person(random, randomAge));
    }

    @Benchmark
    public void updateNode() {
        String random = "User" + (idGenerator.get() % 1000);
        repository.save(new Person(random, (int) (Math.random() * 100)));
    }

    @Benchmark
    public void fetchNode(Blackhole bh) {
        String random = "User" + (idGenerator.get() % 1000);
        Optional<Person> person = repository.findById(random);
        bh.consume(person.get());
    }

    @Benchmark
    public void createRelation() {
        String fromId = "User" + (idGenerator.get() % 500);
        String toId = "User" + ((idGenerator.get() % 500) + 500);
        Follows follows = new Follows();
        follows.since = (int) (Math.random() * 100);
        repository.createRelation(new Person(fromId, 0), new Person(toId, 0), follows);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(LadybugdbBenchmark.class.getSimpleName())
                .forks(1)
                .addProfiler(AsyncProfiler.class, "libPath=/path/to/libasyncProfiler.so;output=flamegraph;dir=results")
                .build();

        new Runner(opt).run();
    }

    interface TestRepository extends NodeRepository<Person, String, Follows, Person> {
    }

    @NodeEntity(label = "Person")
    static class Person {
        @Id
        String name;
        int age;

        Person() {
        }

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    static RowMapper<Person> personReader = (row) -> {
        var p = row.getNode("n");
        String name = ValueMappers.asString(p.get("name"));
        int age = ValueMappers.asInteger(p.get("age"));
        return new Person(name, age);
    };

    static EntityWriter<Person> personWriter = (entity) -> {
        return Map.of("age", entity.age);
    };

    @RelationshipEntity(type = "FOLLOWS", nodeType = Person.class, sourceField = "from", targetField = "to")
    static class Follows {
        @Id
        String name;
        Person from;
        Person to;
        int since;

        Follows() {
        }
    }

    static EntityWriter<Follows> followsWriter = (entity) -> Map.of("since", entity.since);
    static RowMapper<Follows> followsReader = (row) -> new Follows();
}